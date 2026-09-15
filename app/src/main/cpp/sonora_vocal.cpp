/**
 * ==============================================================================
 * MOTOR NATIVO C++ DE PROCESAMIENTO VOCAL DE SONORA (sonora_vocal.cpp)
 * ==============================================================================
 * Implementación de la separación vocal central, modificación de velocidad
 * y corrección de formantes espectrales para evitar el efecto ardilla.
 * ==============================================================================
 */

#include "sonora_vocal.h"
#include <cmath>
#include <cstring>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

SonoraVocalEngine::SonoraVocalEngine() {
    mCircularBuffer.resize(CIRCULAR_SIZE, 0.0f);
    mWindow.resize(GRAIN_SIZE, 0.0f);
    initWindow();
    updateFilters();
}

SonoraVocalEngine::~SonoraVocalEngine() = default;

void SonoraVocalEngine::setSampleRate(float sampleRate) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (sampleRate > 8000.0f && std::abs(mSampleRate - sampleRate) > 1.0f) {
        mSampleRate = sampleRate;
        updateFilters();
    }
}

void SonoraVocalEngine::setEnabled(bool enabled) {
    std::lock_guard<std::mutex> lock(mMutex);
    mEnabled = enabled;
    if (!enabled) {
        reset();
    }
}

bool SonoraVocalEngine::isEnabled() const {
    return mEnabled;
}

void SonoraVocalEngine::setVocalSpeed(float speed) {
    std::lock_guard<std::mutex> lock(mMutex);
    mVocalSpeed = std::max(0.50f, std::min(speed, 2.00f));
    updateFilters();
}

float SonoraVocalEngine::getVocalSpeed() const {
    return mVocalSpeed;
}

void SonoraVocalEngine::setFormantCorrection(bool enabled) {
    std::lock_guard<std::mutex> lock(mMutex);
    mFormantCorrection = enabled;
    updateFilters();
}

bool SonoraVocalEngine::isFormantCorrectionEnabled() const {
    return mFormantCorrection;
}

void SonoraVocalEngine::setVocalIsolation(float isolation) {
    std::lock_guard<std::mutex> lock(mMutex);
    mVocalIsolation = std::max(0.0f, std::min(isolation, 1.0f));
}

float SonoraVocalEngine::getVocalIsolation() const {
    return mVocalIsolation;
}

void SonoraVocalEngine::setVocalGainDb(float gainDb) {
    std::lock_guard<std::mutex> lock(mMutex);
    float clamped = std::max(-6.0f, std::min(gainDb, 6.0f));
    mVocalGainLinear = std::pow(10.0f, clamped / 20.0f);
}

float SonoraVocalEngine::getVocalGainDb() const {
    return 20.0f * std::log10(mVocalGainLinear);
}

void SonoraVocalEngine::reset() {
    std::fill(mCircularBuffer.begin(), mCircularBuffer.end(), 0.0f);
    mWritePos = 0;
    mReadPos = 0.0f;
    mBandpassFilter.reset();
    mFormantCompFilter1.reset();
    mFormantCompFilter2.reset();
}

void SonoraVocalEngine::initWindow() {
    for (size_t i = 0; i < GRAIN_SIZE; ++i) {
        // Ventana Von Hann (Hanning)
        mWindow[i] = 0.5f * (1.0f - std::cos(2.0f * static_cast<float>(M_PI) * static_cast<float>(i) / (GRAIN_SIZE - 1)));
    }
}

/**
 * Calcula coeficientes de filtro para aislamiento vocal y corrección de formantes (Anti-Ardilla).
 */
void SonoraVocalEngine::updateFilters() {
    // 1. Filtro Paso-Banda para aislar el rango de voz humana (300 Hz a 3400 Hz)
    float centerFreq = 1400.0f;
    float qBandpass = 0.75f;
    float w0 = 2.0f * static_cast<float>(M_PI) * centerFreq / mSampleRate;
    float alpha = std::sin(w0) / (2.0f * qBandpass);

    float b0_bp = alpha;
    float b1_bp = 0.0f;
    float b2_bp = -alpha;
    float a0_bp = 1.0f + alpha;
    float a1_bp = -2.0f * std::cos(w0);
    float a2_bp = 1.0f - alpha;

    mBandpassFilter.b0 = b0_bp / a0_bp;
    mBandpassFilter.b1 = b1_bp / a0_bp;
    mBandpassFilter.b2 = b2_bp / a0_bp;
    mBandpassFilter.a1 = a1_bp / a0_bp;
    mBandpassFilter.a2 = a2_bp / a0_bp;

    // 2. Filtros de Corrección de Formantes Espectrales (Anti-Ardilla)
    // Cuando la velocidad aumenta (speed > 1.0), los formantes suben. Compensamos atenuando
    // el exceso en medios-altos (3000 Hz) y realzando los formantes basales (F1 a 500 Hz y F2 a 1500 Hz).
    if (mFormantCorrection && std::abs(mVocalSpeed - 1.0f) > 0.02f) {
        float speedRatio = mVocalSpeed;
        // Ganancia de compensación para Formante 1 (F1: calidez bucal)
        float f1GainDb = (speedRatio > 1.0f) ? ((speedRatio - 1.0f) * 4.5f) : ((speedRatio - 1.0f) * 3.0f);
        // Ganancia de compensación para Formante 2 (F2: claridad fonética sin estridencia de ardilla)
        float f2GainDb = (speedRatio > 1.0f) ? (-(speedRatio - 1.0f) * 5.0f) : ((1.0f - speedRatio) * 3.5f);

        // Biquad Peaking EQ para F1 (~550 Hz)
        {
            float A = std::pow(10.0f, f1GainDb / 40.0f);
            float wF1 = 2.0f * static_cast<float>(M_PI) * 550.0f / mSampleRate;
            float aF1 = std::sin(wF1) / (2.0f * 1.5f);
            float b0 = 1.0f + aF1 * A;
            float b1 = -2.0f * std::cos(wF1);
            float b2 = 1.0f - aF1 * A;
            float a0 = 1.0f + aF1 / A;
            float a1 = -2.0f * std::cos(wF1);
            float a2 = 1.0f - aF1 / A;

            mFormantCompFilter1.b0 = b0 / a0;
            mFormantCompFilter1.b1 = b1 / a0;
            mFormantCompFilter1.b2 = b2 / a0;
            mFormantCompFilter1.a1 = a1 / a0;
            mFormantCompFilter1.a2 = a2 / a0;
        }

        // Biquad Peaking EQ para F2 / Anti-Ardilla (~2200 Hz)
        {
            float A = std::pow(10.0f, f2GainDb / 40.0f);
            float wF2 = 2.0f * static_cast<float>(M_PI) * 2200.0f / mSampleRate;
            float aF2 = std::sin(wF2) / (2.0f * 1.2f);
            float b0 = 1.0f + aF2 * A;
            float b1 = -2.0f * std::cos(wF2);
            float b2 = 1.0f - aF2 * A;
            float a0 = 1.0f + aF2 / A;
            float a1 = -2.0f * std::cos(wF2);
            float a2 = 1.0f - aF2 / A;

            mFormantCompFilter2.b0 = b0 / a0;
            mFormantCompFilter2.b1 = b1 / a0;
            mFormantCompFilter2.b2 = b2 / a0;
            mFormantCompFilter2.a1 = a1 / a0;
            mFormantCompFilter2.a2 = a2 / a0;
        }
    } else {
        // Modo transparente / bypass de filtros de formante
        mFormantCompFilter1.b0 = 1.0f;
        mFormantCompFilter1.b1 = mFormantCompFilter1.b2 = mFormantCompFilter1.a1 = mFormantCompFilter1.a2 = 0.0f;
        mFormantCompFilter2.b0 = 1.0f;
        mFormantCompFilter2.b1 = mFormantCompFilter2.b2 = mFormantCompFilter2.a1 = mFormantCompFilter2.a2 = 0.0f;
    }
}

/**
 * Procesa un buffer de audio estéreo interleaved PCM de 16 bits.
 */
void SonoraVocalEngine::processInterleavedPcm16(int16_t* buffer, int numFrames, int channelCount) {
    if (!mEnabled || numFrames <= 0) return;

    std::lock_guard<std::mutex> lock(mMutex);

    if (channelCount >= 2) {
        for (int i = 0; i < numFrames; ++i) {
            int leftIdx = i * channelCount;
            int rightIdx = leftIdx + 1;

            float inL = static_cast<float>(buffer[leftIdx]) / 32768.0f;
            float inR = static_cast<float>(buffer[rightIdx]) / 32768.0f;

            // 1. Extracción del canal central (Voz y solistas en el centro)
            float center = 0.5f * (inL + inR);

            // 2. Filtro paso-banda vocal para separar la voz humana
            float rawVocal = mBandpassFilter.process(center);

            // 3. Componentes instrumentales laterales que permanecen intactas
            float instL = inL - (mVocalIsolation * rawVocal);
            float instR = inR - (mVocalIsolation * rawVocal);

            // 4. Escritura en el buffer circular de retardo vocal
            mCircularBuffer[mWritePos] = rawVocal;
            mWritePos = (mWritePos + 1) % CIRCULAR_SIZE;

            // 5. Lectura con tasa de velocidad vocal (Time-Stretching OLA)
            float readIndex = mReadPos;
            int idx0 = static_cast<int>(readIndex) % CIRCULAR_SIZE;
            int idx1 = (idx0 + 1) % CIRCULAR_SIZE;
            float frac = readIndex - std::floor(readIndex);

            // Interpolación lineal suave entre muestras
            float processedVocal = mCircularBuffer[idx0] * (1.0f - frac) + mCircularBuffer[idx1] * frac;

            // Avance del puntero de lectura según la velocidad de la voz
            mReadPos += mVocalSpeed;
            if (mReadPos >= static_cast<float>(CIRCULAR_SIZE)) {
                mReadPos -= static_cast<float>(CIRCULAR_SIZE);
            }

            // Mantenimiento de anclaje de fase para que no se desfase indefinidamente
            float lag = static_cast<float>(mWritePos) - mReadPos;
            if (lag < 0.0f) lag += static_cast<float>(CIRCULAR_SIZE);
            if (lag > static_cast<float>(CIRCULAR_SIZE / 2)) {
                // Suavizado de sincronización con la ventana de audio
                mReadPos = static_cast<float>((mWritePos + CIRCULAR_SIZE - (GRAIN_SIZE * 2)) % CIRCULAR_SIZE);
            }

            // 6. Corrección de Formantes (Anti-Ardilla)
            if (mFormantCorrection) {
                processedVocal = mFormantCompFilter1.process(processedVocal);
                processedVocal = mFormantCompFilter2.process(processedVocal);
            }

            // Aplicar ganancia de presencia vocal
            processedVocal *= (mVocalGainLinear * mVocalIsolation);

            // 7. Recombinación con la base instrumental intacta
            float outL = instL + processedVocal;
            float outR = instR + processedVocal;

            // 8. Limitador Soft-Clipping suave analógico
            outL = std::tanh(outL);
            outR = std::tanh(outR);

            buffer[leftIdx] = static_cast<int16_t>(std::clamp(outL * 32767.0f, -32768.0f, 32767.0f));
            buffer[rightIdx] = static_cast<int16_t>(std::clamp(outR * 32767.0f, -32768.0f, 32767.0f));
        }
    } else {
        // Canal Mono
        for (int i = 0; i < numFrames; ++i) {
            float inM = static_cast<float>(buffer[i]) / 32768.0f;
            float rawVocal = mBandpassFilter.process(inM);
            float inst = inM - (mVocalIsolation * rawVocal);

            mCircularBuffer[mWritePos] = rawVocal;
            mWritePos = (mWritePos + 1) % CIRCULAR_SIZE;

            int idx0 = static_cast<int>(mReadPos) % CIRCULAR_SIZE;
            int idx1 = (idx0 + 1) % CIRCULAR_SIZE;
            float frac = mReadPos - std::floor(mReadPos);

            float processedVocal = mCircularBuffer[idx0] * (1.0f - frac) + mCircularBuffer[idx1] * frac;
            mReadPos += mVocalSpeed;
            if (mReadPos >= static_cast<float>(CIRCULAR_SIZE)) {
                mReadPos -= static_cast<float>(CIRCULAR_SIZE);
            }

            if (mFormantCorrection) {
                processedVocal = mFormantCompFilter1.process(processedVocal);
                processedVocal = mFormantCompFilter2.process(processedVocal);
            }

            processedVocal *= (mVocalGainLinear * mVocalIsolation);
            float outM = std::tanh(inst + processedVocal);
            buffer[i] = static_cast<int16_t>(std::clamp(outM * 32767.0f, -32768.0f, 32767.0f));
        }
    }
}
