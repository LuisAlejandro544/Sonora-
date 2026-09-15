package com.example.sonora.nativeengine

import android.util.Log

/**
 * ==============================================================================
 * PUENTE JNI PARA EL MOTOR NATIVO RUST (libsonora_rust.so)
 * ==============================================================================
 * Proporciona métodos nativos de alto rendimiento escritos en Rust para:
 * 1. Análisis espectral FFT de baja latencia para el visualizador de 60 FPS.
 * 2. Cálculo de decibelios RMS (dBFS) en tiempo real.
 * 3. Hashing de audio rápido de 64 bits para deduplicación y verificación de pistas.
 * ==============================================================================
 */
object SonoraRustBridge {
    private const val TAG = "SonoraRustBridge"
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("sonora_rust")
            isLibraryLoaded = true
            Log.i(TAG, "Librería nativa Rust 'libsonora_rust.so' cargada exitosamente")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "No se pudo cargar libsonora_rust.so: ${e.message}")
            isLibraryLoaded = false
        }
    }

    /**
     * Indica si el binario compilado de Rust está cargado en el proceso.
     */
    fun isAvailable(): Boolean = isLibraryLoaded

    /**
     * Obtiene la descripción detallada del motor Rust (arquitectura y optimizaciones).
     */
    external fun nativeGetRustEngineInfo(): String

    /**
     * Calcula los decibelios dBFS de un búfer de muestras PCM de 16 bits.
     */
    external fun nativeCalculateDecibels(samples: ShortArray): Float

    /**
     * Calcula las magnitudes de frecuencias distribuidas en [numBands] bandas
     * para el visualizador de audio en Compose.
     */
    external fun nativeComputeSpectrum(samples: ShortArray, numBands: Int): FloatArray

    /**
     * Genera un hash FNV-1a de 64 bits sobre el búfer binario de audio.
     */
    external fun nativeComputeAudioHash(data: ByteArray): Long

    /**
     * Extrae título, artista, álbum y presencia de carátula desde el archivo en Rust.
     * Retorna un string JSON con la información: {"title":"...","artist":"...","album":"...","hasCover":true/false}
     */
    external fun nativeExtractAudioMetadata(filePath: String): String

    /**
     * Extrae los bytes crudos de la imagen de carátula incrustada (JPEG o PNG) en Rust.
     */
    external fun nativeExtractCoverArt(filePath: String): ByteArray?

    /**
     * Sanea y limpia metadatos de audio en Rust: elimina prefijos/sufijos de ripeo web,
     * arregla mojibake y normaliza nombres.
     */
    external fun nativeSanitizeTrackMetadata(
        title: String,
        artist: String,
        album: String,
        filePath: String
    ): String

    /**
     * Modelo de datos para metadatos de audio extraídos por el motor Rust.
     */
    data class NativeMetadataResult(
        val title: String?,
        val artist: String?,
        val album: String?,
        val hasCover: Boolean
    )

    /**
     * Resultado del saneamiento inteligente de metadatos procesado en Rust.
     */
    data class CleanedMetadataResult(
        val title: String,
        val artist: String,
        val album: String,
        val wasModified: Boolean
    )

    /**
     * Ejecuta el saneamiento y limpieza de metadatos directamente en el motor Rust.
     */
    fun sanitizeMetadataSafe(
        title: String,
        artist: String,
        album: String,
        filePath: String
    ): CleanedMetadataResult? {
        if (!isAvailable()) return null
        return try {
            val jsonStr = nativeSanitizeTrackMetadata(title, artist, album, filePath)
            val json = org.json.JSONObject(jsonStr)
            CleanedMetadataResult(
                title = json.optString("title", title),
                artist = json.optString("artist", artist),
                album = json.optString("album", album),
                wasModified = json.optBoolean("wasModified", false)
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Saneamiento de metadatos en Rust delegado o con error: ${e.message}")
            null
        }
    }

    /**
     * Ejecuta la extracción de metadatos de audio de forma segura.
     */
    fun extractMetadataSafe(filePath: String): NativeMetadataResult? {
        if (!isAvailable()) return null
        return try {
            val jsonStr = nativeExtractAudioMetadata(filePath)
            val json = org.json.JSONObject(jsonStr)
            NativeMetadataResult(
                title = json.optString("title").takeIf { it.isNotBlank() },
                artist = json.optString("artist").takeIf { it.isNotBlank() },
                album = json.optString("album").takeIf { it.isNotBlank() },
                hasCover = json.optBoolean("hasCover", false)
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Extracción de metadatos en Rust delegada o con error: ${e.message}")
            null
        }
    }

    /**
     * Ejecuta la extracción de carátula incrustada de forma segura en Rust.
     */
    fun extractCoverArtSafe(filePath: String): ByteArray? {
        if (!isAvailable()) return null
        return try {
            nativeExtractCoverArt(filePath)
        } catch (e: Throwable) {
            Log.w(TAG, "Extracción de carátula en Rust delegada o con error: ${e.message}")
            null
        }
    }
}

