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
#include <mutex>
#include <android/log.h>

#define TAG "SonoraDspNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// Cantidad estándar de bandas de ecualización ISO (10 bandas por octava)
constexpr int NUM_EQ_BANDS = 10;

// Frecuencias centrales fijas para el ecualizador gráfico de 10 bandas (en Hz)
const float EQ_FREQUENCIES[NUM_EQ_BANDS] = {
    31.25f,   // Sub-graves profundos
    62.5f,    // Graves y pegada de bombo
    125.0f,   // Graves superiores y bajo eléctrico
    250.0f,   // Medios bajos y calidez
    500.0f,   // Cuerpo vocal e instrumental
    1000.0f,  // Medios centrales y claridad
    2000.0f,  // Presencia y consonantes vocales
    4000.0f,  // Ataque y definición de percusión
    8000.0f,  // Agudos y brillo de platillos
    16000.0f  // Aire y respuesta ultra-alta
};

// Estructura para almacenar los 5 coeficientes normalizados de un filtro Biquad
struct BiquadCoefficients {
    float b0, b1, b2;
    float a1, a2;
};

// Estado interno de retardo para un filtro Biquad IIR (Forma Directa I)
struct BiquadState {
    float x1 = 0.0f;
    float x2 = 0.0f;
    float y1 = 0.0f;
    float y2 = 0.0f;

    void reset() {
        x1 = 0.0f;
        x2 = 0.0f;
        y1 = 0.0f;
        y2 = 0.0f;
    }

    // Procesa una muestra de audio aplicando la ecuación en diferencias del filtro
    inline float process(float in, const BiquadCoefficients& c) {
        float out = c.b0 * in + c.b1 * x1 + c.b2 * x2 - c.a1 * y1 - c.a2 * y2;
        x2 = x1;
        x1 = in;
        y2 = y1;
        y1 = out;
        return out;
    }
};

// Banco de filtros para un canal de audio (10 bandas + 1 filtro Low-Shelf para Bass Boost)
struct ChannelFilterBank {
    BiquadState bands[NUM_EQ_BANDS];
    BiquadState bassBoostFilter;

    void reset() {
        for (int i = 0; i < NUM_EQ_BANDS; ++i) {
            bands[i].reset();
        }
        bassBoostFilter.reset();
    }
};

// Instancia global de filtros para Canal Izquierdo y Canal Derecho (Estéreo)
static ChannelFilterBank gLeftChannel;
static ChannelFilterBank gRightChannel;
static std::mutex gDspMutex;

/**
 * Calcula los coeficientes de un filtro biquad paramétrico Peaking EQ
 * basado en las fórmulas del Audio EQ Cookbook de Robert Bristow-Johnson.
 *
 * @param frequency Frecuencia central de corte en Hz
 * @param sampleRate Frecuencia de muestreo (ej: 44100 o 48000 Hz)
 * @param gainDb Ganancia en decibelios (-12.0 dB a +12.0 dB)
 * @param q Factor de calidad Q (ancho de banda, típicamente 1.414 para 1 octava)
 */
static BiquadCoefficients calculatePeakingBiquad(float frequency, float sampleRate, float gainDb, float q) {
    BiquadCoefficients coeffs{1.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    if (sampleRate <= 0.0f || frequency <= 0.0f) {
        return coeffs;
    }

    // Si la ganancia es prácticamente cero, el filtro es transparente
    if (std::abs(gainDb) < 0.02f) {
        return coeffs;
    }

    float A = std::pow(10.0f, gainDb / 40.0f);
    float omega = 2.0f * static_cast<float>(M_PI) * frequency / sampleRate;
    float sn = std::sin(omega);
    float cs = std::cos(omega);
    float alpha = sn / (2.0f * (q > 0.01f ? q : 1.414f));

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

/**
 * Calcula un filtro Low-Shelf (estante de graves) para el refuerzo Bass Boost.
 */
static BiquadCoefficients calculateLowShelf(float frequency, float sampleRate, float gainDb) {
    BiquadCoefficients coeffs{1.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    if (sampleRate <= 0.0f || frequency <= 0.0f || std::abs(gainDb) < 0.05f) {
        return coeffs;
    }

    float A = std::pow(10.0f, gainDb / 40.0f);
    float omega = 2.0f * static_cast<float>(M_PI) * frequency / sampleRate;
    float sn = std::sin(omega);
    float cs = std::cos(omega);
    float alpha = sn / 2.0f * 0.7071f; // Q = 0.707 (Butterworth)
    float twoSqrtAAlpha = 2.0f * std::sqrt(A) * alpha;

    float b0 = A * ((A + 1.0f) - (A - 1.0f) * cs + twoSqrtAAlpha);
    float b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cs);
    float b2 = A * ((A + 1.0f) - (A - 1.0f) * cs - twoSqrtAAlpha);
    float a0 = (A + 1.0f) + (A - 1.0f) * cs + twoSqrtAAlpha;
    float a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cs);
    float a2 = (A + 1.0f) + (A - 1.0f) * cs - twoSqrtAAlpha;

    coeffs.b0 = b0 / a0;
    coeffs.b1 = b1 / a0;
    coeffs.b2 = b2 / a0;
    coeffs.a1 = a1 / a0;
    coeffs.a2 = a2 / a0;

    return coeffs;
}

/**
 * Función interna de procesamiento de una muestra de audio a través de la cadena DSP.
 */
static inline float processSampleChain(
    float sample,
    ChannelFilterBank& channelBank,
    const BiquadCoefficients bandCoeffs[NUM_EQ_BANDS],
    const BiquadCoefficients& bassCoeffs,
    bool bassActive,
    float preampLinear,
    bool softClipEnabled
) {
    // 1. Ganancia de Preamplificación
    float s = sample * preampLinear;

    // 2. Refuerzo Bass Boost si está activo
    if (bassActive) {
        s = channelBank.bassBoostFilter.process(s, bassCoeffs);
    }

    // 3. Cadena secuencial de las 10 bandas del ecualizador
    for (int b = 0; b < NUM_EQ_BANDS; ++b) {
        s = channelBank.bands[b].process(s, bandCoeffs[b]);
    }

    // 4. Saturación y limitación suave
    if (softClipEnabled) {
        // Compresión suave analógica con tangente hiperbólica (estilo válvulas de audio)
        s = std::tanh(s * 1.15f) / std::tanh(1.15f);
    } else {
        // Limitador duro para prevenir clipping digital estricto
        if (s > 1.0f) s = 1.0f;
        else if (s < -1.0f) s = -1.0f;
    }

    return s;
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

    std::string info = "Sonora C++ 10-Band DSP Engine v2.0 | Arquitectura: " + arch + " | Robert Bristow-Johnson IIR + SoftClip";
    return env->NewStringUTF(info.c_str());
}

/**
 * Reinicia los estados de retardo de todas las bandas del ecualizador.
 */
JNIEXPORT void JNICALL
Java_com_example_sonora_nativeengine_SonoraCppBridge_nativeReset10BandEqualizer(
        JNIEnv * /* env */,
        jclass /* clazz */) {
    std::lock_guard<std::mutex> lock(gDspMutex);
    gLeftChannel.reset();
    gRightChannel.reset();
}

/**
 * Procesa un ByteBuffer directo de audio PCM de 16 bits conectado al flujo de ExoPlayer.
 * Funciona sin realizar copias en memoria JVM gracias a GetDirectBufferAddress.
 */
JNIEXPORT void JNICALL
Java_com_example_sonora_nativeengine_SonoraCppBridge_nativeProcessDirectPcm10Band(
        JNIEnv *env,
        jclass /* clazz */,
        jobject input_buffer,
        jint input_offset,
        jobject output_buffer,
        jint output_offset,
        jint num_bytes,
        jint sample_rate,
        jint channel_count,
        jfloatArray band_gains_db,
        jfloat preamp_db,
        jfloat bass_boost_db,
        jboolean soft_clip_enabled) {

    if (num_bytes <= 0 || sample_rate <= 0) return;

    void* inAddress = env->GetDirectBufferAddress(input_buffer);
    void* outAddress = env->GetDirectBufferAddress(output_buffer);

    if (inAddress == nullptr || outAddress == nullptr) {
        LOGE("Buffers directos nulos en nativeProcessDirectPcm10Band");
        return;
    }

    auto* inPtr = reinterpret_cast<int16_t*>(static_cast<uint8_t*>(inAddress) + input_offset);
    auto* outPtr = reinterpret_cast<int16_t*>(static_cast<uint8_t*>(outAddress) + output_offset);
    int totalSamples = num_bytes / static_cast<int>(sizeof(int16_t));

    // Obtener ganancias de las 10 bandas
    jsize gainsLen = env->GetArrayLength(band_gains_db);
    std::vector<float> gains(NUM_EQ_BANDS, 0.0f);
    if (gainsLen > 0) {
        jfloat* gainsElements = env->GetFloatArrayElements(band_gains_db, nullptr);
        for (int i = 0; i < std::min(static_cast<int>(gainsLen), NUM_EQ_BANDS); ++i) {
            gains[i] = gainsElements[i];
        }
        env->ReleaseFloatArrayElements(band_gains_db, gainsElements, JNI_ABORT);
    }

    // Calcular coeficientes de las 10 bandas peaking
    BiquadCoefficients bandCoeffs[NUM_EQ_BANDS];
    float sr = static_cast<float>(sample_rate);
    for (int b = 0; b < NUM_EQ_BANDS; ++b) {
        bandCoeffs[b] = calculatePeakingBiquad(EQ_FREQUENCIES[b], sr, gains[b], 1.414f);
    }

    // Coeficiente para Bass Boost (corte a 80 Hz)
    bool bassActive = (bass_boost_db > 0.1f);
    BiquadCoefficients bassCoeffs = calculateLowShelf(80.0f, sr, bass_boost_db);

    float preampLinear = std::pow(10.0f, preamp_db / 20.0f);

    std::lock_guard<std::mutex> lock(gDspMutex);

    if (channel_count == 2) {
        // Audio Estéreo entrelazado (L, R, L, R, ...)
        for (int i = 0; i < totalSamples - 1; i += 2) {
            float inL = static_cast<float>(inPtr[i]) / 32768.0f;
            float inR = static_cast<float>(inPtr[i + 1]) / 32768.0f;

            float outL = processSampleChain(inL, gLeftChannel, bandCoeffs, bassCoeffs, bassActive, preampLinear, soft_clip_enabled);
            float outR = processSampleChain(inR, gRightChannel, bandCoeffs, bassCoeffs, bassActive, preampLinear, soft_clip_enabled);

            outPtr[i] = static_cast<int16_t>(std::clamp(outL * 32767.0f, -32768.0f, 32767.0f));
            outPtr[i + 1] = static_cast<int16_t>(std::clamp(outR * 32767.0f, -32768.0f, 32767.0f));
        }
    } else {
        // Audio Mono
        for (int i = 0; i < totalSamples; ++i) {
            float inM = static_cast<float>(inPtr[i]) / 32768.0f;
            float outM = processSampleChain(inM, gLeftChannel, bandCoeffs, bassCoeffs, bassActive, preampLinear, soft_clip_enabled);
            outPtr[i] = static_cast<int16_t>(std::clamp(outM * 32767.0f, -32768.0f, 32767.0f));
        }
    }
}

/**
 * Procesa un array de muestras PCM de 16 bits con el ecualizador de 10 bandas.
 */
JNIEXPORT void JNICALL
Java_com_example_sonora_nativeengine_SonoraCppBridge_nativeProcessPcm10Band(
        JNIEnv *env,
        jclass /* clazz */,
        jshortArray pcm_samples,
        jint num_samples,
        jint sample_rate,
        jint channel_count,
        jfloatArray band_gains_db,
        jfloat preamp_db,
        jfloat bass_boost_db,
        jboolean soft_clip_enabled) {

    jsize len = env->GetArrayLength(pcm_samples);
    int count = std::min(static_cast<int>(len), num_samples);
    if (count <= 0 || sample_rate <= 0) return;

    std::vector<jshort> buffer(count);
    env->GetShortArrayRegion(pcm_samples, 0, count, buffer.data());

    jsize gainsLen = env->GetArrayLength(band_gains_db);
    std::vector<float> gains(NUM_EQ_BANDS, 0.0f);
    if (gainsLen > 0) {
        jfloat* gainsElements = env->GetFloatArrayElements(band_gains_db, nullptr);
        for (int i = 0; i < std::min(static_cast<int>(gainsLen), NUM_EQ_BANDS); ++i) {
            gains[i] = gainsElements[i];
        }
        env->ReleaseFloatArrayElements(band_gains_db, gainsElements, JNI_ABORT);
    }

    BiquadCoefficients bandCoeffs[NUM_EQ_BANDS];
    float sr = static_cast<float>(sample_rate);
    for (int b = 0; b < NUM_EQ_BANDS; ++b) {
        bandCoeffs[b] = calculatePeakingBiquad(EQ_FREQUENCIES[b], sr, gains[b], 1.414f);
    }

    bool bassActive = (bass_boost_db > 0.1f);
    BiquadCoefficients bassCoeffs = calculateLowShelf(80.0f, sr, bass_boost_db);
    float preampLinear = std::pow(10.0f, preamp_db / 20.0f);

    std::lock_guard<std::mutex> lock(gDspMutex);

    if (channel_count == 2) {
        for (int i = 0; i < count - 1; i += 2) {
            float inL = static_cast<float>(buffer[i]) / 32768.0f;
            float inR = static_cast<float>(buffer[i + 1]) / 32768.0f;

            float outL = processSampleChain(inL, gLeftChannel, bandCoeffs, bassCoeffs, bassActive, preampLinear, soft_clip_enabled);
            float outR = processSampleChain(inR, gRightChannel, bandCoeffs, bassCoeffs, bassActive, preampLinear, soft_clip_enabled);

            buffer[i] = static_cast<jshort>(std::clamp(outL * 32767.0f, -32768.0f, 32767.0f));
            buffer[i + 1] = static_cast<jshort>(std::clamp(outR * 32767.0f, -32768.0f, 32767.0f));
        }
    } else {
        for (int i = 0; i < count; ++i) {
            float inM = static_cast<float>(buffer[i]) / 32768.0f;
            float outM = processSampleChain(inM, gLeftChannel, bandCoeffs, bassCoeffs, bassActive, preampLinear, soft_clip_enabled);
            buffer[i] = static_cast<jshort>(std::clamp(outM * 32767.0f, -32768.0f, 32767.0f));
        }
    }

    env->SetShortArrayRegion(pcm_samples, 0, count, buffer.data());
}

/**
 * MÉTODOS HEREDADOS (Compatibilidad previa)
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
    if (result == nullptr) return nullptr;

    jfloat buffer[5] = { coeffs.b0, coeffs.b1, coeffs.b2, coeffs.a1, coeffs.a2 };
    env->SetFloatArrayRegion(result, 0, 5, buffer);
    return result;
}

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

    float x1 = 0.0f, x2 = 0.0f;
    float y1 = 0.0f, y2 = 0.0f;

    for (int i = 0; i < len; ++i) {
        float x0 = static_cast<float>(buffer[i]) / 32768.0f;
        float y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = x0;
        y2 = y1;
        y1 = y0;

        if (y0 > 1.0f) y0 = 1.0f;
        else if (y0 < -1.0f) y0 = -1.0f;

        buffer[i] = static_cast<jshort>(y0 * 32767.0f);
    }

    env->SetShortArrayRegion(pcm_samples, 0, len, buffer.data());
}

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
        float clipped = std::tanh(sample * safeDrive) / std::tanh(safeDrive);
        buffer[i] = static_cast<jshort>(clipped * 32767.0f);
    }

    env->SetShortArrayRegion(pcm_samples, 0, len, buffer.data());
}

} // extern "C"
