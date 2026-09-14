/**
 * ==============================================================================
 * MOTOR DSP NATIVO SONORA EN C++ (libsonora_dsp.so)
 * ==============================================================================
 * Este archivo implementa el procesamiento de señales de audio digital (DSP):
 * 1. Coeficientes de filtros IIR Biquad (Cookbook de Robert Bristow-Johnson).
 * 2. Procesamiento de búfer PCM estéreo y mono con ecualización en tiempo real.
 * 3. Limitador Soft-Clipping no lineal (tangente hiperbólica) para evitar distorsión.
 * 4. Soporte multi-arquitectura para 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 * ==============================================================================
 */

#include <jni.h>
#include <cmath>
#include <vector>
#include <string>
#include <algorithm>
#include <android/log.h>

#define TAG "SonoraDspNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// Estructura para almacenar los 5 coeficientes normalizados de un filtro Biquad
struct BiquadCoefficients {
    float b0, b1, b2;
    float a1, a2;
};

/**
 * Calcula los coeficientes de un filtro biquad paramétrico Peaking EQ
 * basado en las fórmulas del Audio EQ Cookbook de Robert Bristow-Johnson.
 *
 * @param frequency Frecuencia central de corte (en Hz, ej: 60, 230, 910, 3600, 14000)
 * @param sampleRate Frecuencia de muestreo (típicamente 44100 o 48000 Hz)
 * @param gainDb Ganancia en decibelios (-12.0 dB a +12.0 dB)
 * @param q Factor de calidad Q (ancho de banda, típicamente 1.0f para ecualizadores gráficos)
 */
static BiquadCoefficients calculatePeakingBiquad(float frequency, float sampleRate, float gainDb, float q) {
    BiquadCoefficients coeffs{};
    if (sampleRate <= 0.0f || frequency <= 0.0f) {
        coeffs.b0 = 1.0f;
        coeffs.b1 = 0.0f;
        coeffs.b2 = 0.0f;
        coeffs.a1 = 0.0f;
        coeffs.a2 = 0.0f;
        return coeffs;
    }

    float A = std::pow(10.0f, gainDb / 40.0f);
    float omega = 2.0f * static_cast<float>(M_PI) * frequency / sampleRate;
    float sn = std::sin(omega);
    float cs = std::cos(omega);
    float alpha = sn / (2.0f * (q > 0.01f ? q : 1.0f));

    float b0 = 1.0f + alpha * A;
    float b1 = -2.0f * cs;
    float b2 = 1.0f - alpha * A;
    float a0 = 1.0f + alpha / A;
    float a1 = -2.0f * cs;
    float a2 = 1.0f - alpha / A;

    // Normalización dividiendo por a0
    coeffs.b0 = b0 / a0;
    coeffs.b1 = b1 / a0;
    coeffs.b2 = b2 / a0;
    coeffs.a1 = a1 / a0;
    coeffs.a2 = a2 / a0;

    return coeffs;
}

extern "C" {

/**
 * Devuelve la descripción y arquitectura actual del motor nativo C++.
 */
JNIEXPORT jstring JNICALL
Java_com_example_sonora_nativeengine_SonoraCppBridge_nativeGetCppEngineInfo(
        JNIEnv *env,
        jclass /* clazz */) {
    std::string arch;
#if defined(__aarch64__)
    arch = "ARM64 (arm64-v8a - 64 bits)";
#elif defined(__arm__)
    arch = "ARM32 (armeabi-v7a - 32 bits)";
#elif defined(__x86_64__)
    arch = "x86_64 (64 bits)";
#elif defined(__i386__)
    arch = "x86 (32 bits)";
#else
    arch = "Genérica";
#endif

    std::string info = "Sonora C++ DSP Core v1.0.0 | Arquitectura: " + arch + " | Estándar: C++17 (-O3 Clang NDK)";
    return env->NewStringUTF(info.c_str());
}

/**
 * Calcula los coeficientes de filtro para una banda de ecualizador y los devuelve
 * como un array de floats [b0, b1, b2, a1, a2].
 */
JNIEXPORT jfloatArray JNICALL
Java_com_example_sonora_nativeengine_SonoraCppBridge_nativeCalculateBiquadFilter(
        JNIEnv *env,
        jclass /* clazz */,
        jfloat frequency,
        jfloat sample_rate,
        jfloat gain_db,
        jfloat q) {

    BiquadCoefficients coeffs = calculatePeakingBiquad(frequency, sample_rate, gain_db, q);
    jfloatArray result = env->NewFloatArray(5);
    if (result == nullptr) {
        return nullptr;
    }

    jfloat buffer[5] = { coeffs.b0, coeffs.b1, coeffs.b2, coeffs.a1, coeffs.a2 };
    env->SetFloatArrayRegion(result, 0, 5, buffer);
    return result;
}

/**
 * Aplica el filtro Biquad IIR a un búfer PCM de enteros de 16 bits.
 * Modifica las muestras directamente para obtener máximo rendimiento sin latencia.
 */
JNIEXPORT void JNICALL
Java_com_example_sonora_nativeengine_SonoraCppBridge_nativeProcessPcmBuffer(
        JNIEnv *env,
        jclass /* clazz */,
        jshortArray pcm_samples,
        jfloat b0, jfloat b1, jfloat b2,
        jfloat a1, jfloat a2) {

    jsize len = env->GetArrayLength(pcm_samples);
    if (len <= 0) return;

    std::vector<jshort> buffer(len);
    env->GetShortArrayRegion(pcm_samples, 0, len, buffer.data());

    // Estados de retardo del filtro directo forma II
    float x1 = 0.0f, x2 = 0.0f;
    float y1 = 0.0f, y2 = 0.0f;

    for (int i = 0; i < len; ++i) {
        float x0 = static_cast<float>(buffer[i]) / 32768.0f;
        // Ecuación en diferencias del filtro biquad: y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
        float y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

        x2 = x1;
        x1 = x0;
        y2 = y1;
        y1 = y0;

        // Saturación suave para evitar clipping digital
        if (y0 > 1.0f) y0 = 1.0f;
        else if (y0 < -1.0f) y0 = -1.0f;

        buffer[i] = static_cast<jshort>(y0 * 32767.0f);
    }

    env->SetShortArrayRegion(pcm_samples, 0, len, buffer.data());
}

/**
 * Aplica saturación suave no lineal (Soft-Clipping) basada en tanh
 * para suavizar picos de audio agresivos producidos por Bass Boost.
 */
JNIEXPORT void JNICALL
Java_com_example_sonora_nativeengine_SonoraCppBridge_nativeApplySoftClip(
        JNIEnv *env,
        jclass /* clazz */,
        jshortArray pcm_samples,
        jfloat drive) {

    jsize len = env->GetArrayLength(pcm_samples);
    if (len <= 0) return;

    std::vector<jshort> buffer(len);
    env->GetShortArrayRegion(pcm_samples, 0, len, buffer.data());

    float safeDrive = std::max(1.0f, std::min(drive, 5.0f));

    for (int i = 0; i < len; ++i) {
        float sample = static_cast<float>(buffer[i]) / 32768.0f;
        // Curva suave de compresión analógica estilo válvulas
        float clipped = std::tanh(sample * safeDrive) / std::tanh(safeDrive);
        buffer[i] = static_cast<jshort>(clipped * 32767.0f);
    }

    env->SetShortArrayRegion(pcm_samples, 0, len, buffer.data());
}

} // extern "C"
