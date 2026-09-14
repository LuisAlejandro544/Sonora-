package com.example.sonora.nativeengine

import android.os.Build
import android.util.Log

/**
 * ==============================================================================
 * GESTOR DE MOTORES NATIVOS (NativeEngineManager)
 * ==============================================================================
 * Centraliza la interacción entre la interfaz Kotlin/Compose y las librerías nativas:
 * - Detección de ABI del dispositivo (arm64-v8a, armeabi-v7a, x86_64, x86).
 * - Identificación de 32 bits vs 64 bits.
 * - Monitoreo de estado de carga de C++ y Rust.
 * - Ejecución de diagnósticos y test de rendimiento nativo en tiempo real.
 * ==============================================================================
 */
data class NativeEngineStatus(
    val rustLoaded: Boolean,
    val rustInfo: String,
    val cppLoaded: Boolean,
    val cppInfo: String,
    val deviceAbi: String,
    val is64Bit: Boolean,
    val lastBenchmarkResult: String = ""
)

object NativeEngineManager {
    private const val TAG = "NativeEngineManager"

    /**
     * Obtiene el estado actual de ambos motores nativos y la arquitectura del procesador.
     */
    fun getStatus(): NativeEngineStatus {
        val primaryAbi = if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "desconocida"
        val is64Bit = primaryAbi.contains("64") || primaryAbi.contains("arm64")

        val rustLoaded = SonoraRustBridge.isAvailable()
        val rustInfo = if (rustLoaded) {
            try {
                SonoraRustBridge.nativeGetRustEngineInfo()
            } catch (e: Throwable) {
                "Error al invocar Rust: ${e.message}"
            }
        } else {
            "Motor Rust no disponible (comprobar ABI: $primaryAbi)"
        }

        val cppLoaded = SonoraCppBridge.isAvailable()
        val cppInfo = if (cppLoaded) {
            try {
                SonoraCppBridge.nativeGetCppEngineInfo()
            } catch (e: Throwable) {
                "Error al invocar C++: ${e.message}"
            }
        } else {
            "Motor C++ no disponible (comprobar ABI: $primaryAbi)"
        }

        return NativeEngineStatus(
            rustLoaded = rustLoaded,
            rustInfo = rustInfo,
            cppLoaded = cppLoaded,
            cppInfo = cppInfo,
            deviceAbi = primaryAbi,
            is64Bit = is64Bit
        )
    }

    /**
     * Ejecuta una prueba de rendimiento comparativa en vivo entre el cálculo de
     * espectrograma FFT (Rust) y el filtrado biquad IIR + saturación tanh (C++).
     *
     * @return Resumen con tiempos en microsegundos y validación de salida.
     */
    fun runPerformanceBenchmark(): String {
        val samplesCount = 1024
        // Generar señal de prueba sinusoidal sintética
        val testPcm = ShortArray(samplesCount) { i ->
            (Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0) * 16000.0).toInt().toShort()
        }

        val sb = StringBuilder()
        sb.append("--- RESULTADOS DEL BENCHMARK NATIVO ---\n")

        // 1. Benchmark de Rust
        if (SonoraRustBridge.isAvailable()) {
            val startRust = System.nanoTime()
            val spectrum = SonoraRustBridge.nativeComputeSpectrum(testPcm, 16)
            val dbfs = SonoraRustBridge.nativeCalculateDecibels(testPcm)
            val testBytes = ByteArray(1024) { (it % 256).toByte() }
            val hash = SonoraRustBridge.nativeComputeAudioHash(testBytes)
            val elapsedRustUs = (System.nanoTime() - startRust) / 1000

            sb.append("• Rust (FFT + dBFS + Hash 64b):\n")
            sb.append("  Tiempo: ${elapsedRustUs} µs | Bandas calculadas: ${spectrum.size}\n")
            sb.append("  Nivel RMS: ${String.format("%.1f", dbfs)} dBFS | Hash: 0x${java.lang.Long.toHexString(hash)}\n")
        } else {
            sb.append("• Rust: Motor no cargado\n")
        }

        // 2. Benchmark de C++
        if (SonoraCppBridge.isAvailable()) {
            val startCpp = System.nanoTime()
            val coeffs = SonoraCppBridge.nativeCalculateBiquadFilter(
                frequency = 1000f,
                sampleRate = 44100f,
                gainDb = 6.0f,
                q = 1.0f
            )
            val pcmCopy = testPcm.clone()
            if (coeffs != null && coeffs.size >= 5) {
                SonoraCppBridge.nativeProcessPcmBuffer(
                    pcmCopy,
                    coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4]
                )
            }
            SonoraCppBridge.nativeApplySoftClip(pcmCopy, 1.5f)
            val elapsedCppUs = (System.nanoTime() - startCpp) / 1000

            sb.append("• C++ (Filtro Biquad IIR + Limitador SoftClip):\n")
            sb.append("  Tiempo: ${elapsedCppUs} µs | Coeficientes: [${coeffs?.joinToString(", ") { String.format("%.3f", it) }}]\n")
        } else {
            sb.append("• C++: Motor no cargado\n")
        }

        val result = sb.toString()
        Log.i(TAG, result)
        return result
    }
}
