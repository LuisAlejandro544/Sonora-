package com.example.sonora.nativeengine

import android.util.Log

/**
 * ==============================================================================
 * PUENTE JNI PARA EL MOTOR DSP NATIVO C++ (libsonora_dsp.so)
 * ==============================================================================
 * Proporciona métodos de procesamiento de señales de audio digital (DSP):
 * 1. Cálculo de coeficientes de filtro biquad paramétrico (Cookbook de RBJ).
 * 2. Filtrado directo de búferes PCM en memoria con mínima sobrecarga.
 * 3. Limitador analógico Soft-Clipping (tanh) para evitar saturación en Bass Boost.
 * ==============================================================================
 */
object SonoraCppBridge {
    private const val TAG = "SonoraCppBridge"
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("sonora_dsp")
            isLibraryLoaded = true
            Log.i(TAG, "Librería nativa C++ 'libsonora_dsp.so' cargada exitosamente")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "No se pudo cargar libsonora_dsp.so: ${e.message}")
            isLibraryLoaded = false
        }
    }

    /**
     * Indica si el binario compilado de C++ está cargado en el proceso.
     */
    fun isAvailable(): Boolean = isLibraryLoaded

    /**
     * Obtiene la descripción detallada del motor C++ (arquitectura y versión Clang).
     */
    external fun nativeGetCppEngineInfo(): String

    /**
     * Calcula los coeficientes de un filtro biquad Peaking EQ [b0, b1, b2, a1, a2].
     */
    external fun nativeCalculateBiquadFilter(
        frequency: Float,
        sampleRate: Float,
        gainDb: Float,
        q: Float
    ): FloatArray?

    /**
     * Procesa un búfer PCM de 16 bits aplicando los coeficientes calculados.
     */
    external fun nativeProcessPcmBuffer(
        pcmSamples: ShortArray,
        b0: Float,
        b1: Float,
        b2: Float,
        a1: Float,
        a2: Float
    )

    /**
     * Aplica distorsión suave y protección de saturación por tanh.
     */
    external fun nativeApplySoftClip(pcmSamples: ShortArray, drive: Float)
}
