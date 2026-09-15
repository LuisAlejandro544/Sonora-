/**
 * ==============================================================================
 * MOTOR NATIVO C++ DE PROCESAMIENTO VOCAL DE SONORA (sonora_vocal.h)
 * ==============================================================================
 * Implementa:
 * 1. Separación de canal central vocal (Center-Channel Vocal Separation).
 * 2. Modificación de escala temporal vocal (Time-Scale Modification) por OLA granular.
 * 3. Corrección espectral de formantes humanos (Anti-Ardilla / Formant Tracking).
 * 4. Compatibilidad estricta con arquitecturas de 32 bits y 64 bits.
 * ==============================================================================
 */

#ifndef SONORA_VOCAL_H
#define SONORA_VOCAL_H

#include <vector>
#include <cmath>
#include <cstdint>
#include <algorithm>
#include <mutex>

class SonoraVocalEngine {
public:
    SonoraVocalEngine();
    ~SonoraVocalEngine();

    // Configuración de frecuencia de muestreo (44100 / 48000 Hz)
    void setSampleRate(float sampleRate);

    // Activar o desactivar el procesamiento vocal
    void setEnabled(bool enabled);
    bool isEnabled() const;

    // Velocidad de la voz (0.50x a 2.00x)
    void setVocalSpeed(float speed);
    float getVocalSpeed() const;

    // Corrección de formantes para evitar efecto ardilla
    void setFormantCorrection(bool enabled);
    bool isFormantCorrectionEnabled() const;

    // Aislamiento vocal del canal central (0.0 a 1.0)
    void setVocalIsolation(float isolation);
    float getVocalIsolation() const;

    // Ganancia adicional para la voz procesada (-6.0 dB a +6.0 dB)
    void setVocalGainDb(float gainDb);
    float getVocalGainDb() const;

    // Reinicia los buffers y retardos internos
    void reset();

    // Procesa un buffer de audio estéreo entrelazado (Interleaved PCM 16-bit)
    void processInterleavedPcm16(int16_t* buffer, int numFrames, int channelCount);

    // Procesa buffers flotantes directos estéreo
    void processStereoFloat(float* left, float* right, int numFrames);

private:
    std::mutex mMutex;
    float mSampleRate = 44100.0f;
    bool mEnabled = false;
    float mVocalSpeed = 1.0f;
    bool mFormantCorrection = true;
    float mVocalIsolation = 0.85f;
    float mVocalGainLinear = 1.0f;

    // Filtros Biquad para separación vocal (Paso-banda de 300 Hz a 3400 Hz)
    struct Biquad {
        float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
        float a1 = 0.0f, a2 = 0.0f;
        float x1 = 0.0f, x2 = 0.0f;
        float y1 = 0.0f, y2 = 0.0f;

        void reset() {
            x1 = x2 = y1 = y2 = 0.0f;
        }

        inline float process(float in) {
            float out = b0 * in + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = in;
            y2 = y1;
            y1 = out;
            return out;
        }
    };

    Biquad mBandpassFilter;     // Aísla la región vocal de la mezcla
    Biquad mFormantCompFilter1; // Filtro de compensación de formante F1 (~500 Hz)
    Biquad mFormantCompFilter2; // Filtro de compensación de formante F2 (~1800 Hz)

    // Buffer circular para Time-Stretching granular vocal
    static constexpr size_t GRAIN_SIZE = 1024;      // ~23ms a 44.1kHz
    static constexpr size_t CIRCULAR_SIZE = 16384;  // Buffer de retardo
    std::vector<float> mCircularBuffer;
    std::vector<float> mWindow;
    size_t mWritePos = 0;
    float mReadPos = 0.0f;

    void updateFilters();
    void initWindow();
};

#endif // SONORA_VOCAL_H
