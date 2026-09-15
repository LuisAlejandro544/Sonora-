package com.example.sonora.nativeengine

import android.util.Log
import java.nio.ByteBuffer

/**
 * ==============================================================================
 * PUENTE JNI PARA EL MOTOR DSP NATIVO C++ (libsonora_dsp.so)
 * ==============================================================================
 * Proporciona métodos de procesamiento de señales de audio digital (DSP):
 * 1. Ecualizador gráfico de 10 bandas ISO en tiempo real conectado a ExoPlayer.
 * 2. Procesamiento de búferes directos PCM (ByteBuffers) con cero copias en memoria.
 * 3. Refuerzo de graves dinámico (Low-Shelf) y limitador analógico Soft-Clipping (tanh).
 * 4. Cálculo de coeficientes de filtro biquad paramétrico (Cookbook de RBJ).
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
     * Obtiene de forma segura la información del motor nativo C++.
     */
    fun getEngineInfo(): String {
        return if (isLibraryLoaded) {
            try {
                nativeGetCppEngineInfo()
            } catch (e: Throwable) {
                "Sonora C++ DSP Activo"
            }
        } else {
            "Motor C++ no inicializado"
        }
    }

    /**
     * Obtiene la descripción detallada del motor C++ (arquitectura y versión Clang).
     */
    external fun nativeGetCppEngineInfo(): String

    /**
     * Reinicia los estados de los 10 filtros biquad en C++ (limpia retardos).
     */
    external fun nativeReset10BandEqualizer()

    /**
     * Procesa un búfer directo (DirectByteBuffer) de PCM 16 bits sin sobrecarga de copias JNI.
     */
    external fun nativeProcessDirectPcm10Band(
        inputBuffer: ByteBuffer,
        inputOffset: Int,
        outputBuffer: ByteBuffer,
        outputOffset: Int,
        numBytes: Int,
        sampleRate: Int,
        channelCount: Int,
        bandGainsDb: FloatArray,
        preampDb: Float,
        bassBoostDb: Float,
        softClipEnabled: Boolean
    )

    /**
     * Procesa un arreglo de enteros cortos (PCM 16-bit) con el ecualizador nativo de 10 bandas.
     */
    external fun nativeProcessPcm10Band(
        pcmSamples: ShortArray,
        numSamples: Int,
        sampleRate: Int,
        channelCount: Int,
        bandGainsDb: FloatArray,
        preampDb: Float,
        bassBoostDb: Float,
        softClipEnabled: Boolean
    )

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

    /**
     * ==============================================================================
     * MOTOR VOCAL NATIVO C++ (Time-Scale Modification & Anti-Ardilla)
     * ==============================================================================
     */

    /**
     * Configura los parámetros de procesamiento del motor vocal en tiempo real.
     */
    external fun nativeSetVocalEngineConfig(
        enabled: Boolean,
        speed: Float,
        formantCorrection: Boolean,
        isolation: Float,
        gainDb: Float
    )

    /**
     * Reinicia buffers circulares y estados de filtros de formante.
     */
    external fun nativeResetVocalEngine()

    /**
     * Procesa un DirectByteBuffer PCM 16-bit con el motor vocal nativo C++.
     */
    external fun nativeProcessDirectVocal(
        buffer: ByteBuffer,
        offset: Int,
        numBytes: Int,
        sampleRate: Int,
        channelCount: Int
    )

    /**
     * Procesa un array de enteros cortos (PCM 16-bit) con el motor vocal nativo C++.
     */
    external fun nativeProcessVocalPcm(
        pcmSamples: ShortArray,
        numSamples: Int,
        sampleRate: Int,
        channelCount: Int
    )
}
