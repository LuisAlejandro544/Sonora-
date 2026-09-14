package com.example.data.storage

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * ==============================================================================
 * GESTOR DE ALMACENAMIENTO MODULAR DE SONORA (SonoraStorageManager)
 * ==============================================================================
 * Administra el almacenamiento persistente desacoplado en la ruta de la app
 * (android/data/com.nuestraapp/files/):
 *
 * Estructura de carpetas:
 * 1. [canciones/]       -> Almacena los archivos de audio importados (mp3, flac, wav, etc.)
 * 2. [webp/]            -> Almacena las carátulas comprimidas a WebP sin pérdida (Lossless)
 * 3. [metadatos/]       -> Almacena archivos de texto con el nombre de la canción, artista y álbum
 * 4. [registros_json/]  -> Almacena archivos .json estructurados que conectan los 3 elementos
 *                          anteriores para mantener un registro y sincronización permanente.
 * ==============================================================================
 */
class SonoraStorageManager(private val context: Context) {

    companion object {
        private const val TAG = "SonoraStorageManager"
        const val DIR_SONGS = "canciones"
        const val DIR_WEBP = "webp"
        const val DIR_METADATA = "metadatos"
        const val DIR_JSON_RECORDS = "registros_json"
    }

    /**
     * Directorio base prioritario en android/data/com.nuestraapp/files/
     * Con fallback a almacenamiento interno privado (context.filesDir) si el externo no está disponible.
     */
    val baseDir: File by lazy {
        val external = context.getExternalFilesDir(null)
        val dir = external ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /** Carpeta para guardar los archivos de audio */
    val songsDir: File by lazy {
        File(baseDir, DIR_SONGS).apply { if (!exists()) mkdirs() }
    }

    /** Carpeta para guardar las carátulas en formato WebP Lossless */
    val webpDir: File by lazy {
        File(baseDir, DIR_WEBP).apply { if (!exists()) mkdirs() }
    }

    /** Carpeta para guardar los metadatos de texto (nombre de canción, artista, álbum) */
    val metadataDir: File by lazy {
        File(baseDir, DIR_METADATA).apply { if (!exists()) mkdirs() }
    }

    /** Carpeta para guardar los archivos .json que enlazan las canciones, webp y metadatos */
    val jsonRecordsDir: File by lazy {
        File(baseDir, DIR_JSON_RECORDS).apply { if (!exists()) mkdirs() }
    }

    /**
     * Modelo de datos que representa el enlace conector entre las partes separadas.
     */
    data class AudioTrackRecord(
        val trackUuid: String,
        val title: String,
        val artist: String,
        val album: String,
        val songFilePath: String,
        val webpCoverPath: String?,
        val metadataFilePath: String,
        val jsonRecordPath: String,
        val durationMs: Long,
        val fileSize: Long,
        val mimeType: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Guarda el archivo de metadatos en texto claro en la carpeta [metadatos/].
     */
    fun saveMetadataFile(trackUuid: String, title: String, artist: String, album: String, durationMs: Long): File {
        val metaFile = File(metadataDir, "meta_${trackUuid}.txt")
        val content = buildString {
            appendLine("=== METADATOS DE PISTA - SONORA ===")
            appendLine("ID_PISTA: $trackUuid")
            appendLine("TITULO: $title")
            appendLine("ARTISTA: $artist")
            appendLine("ALBUM: $album")
            appendLine("DURACION_MS: $durationMs")
            appendLine("FECHA_REGISTRO: ${System.currentTimeMillis()}")
        }
        metaFile.writeText(content, Charsets.UTF_8)
        return metaFile
    }

    /**
     * Guarda el archivo conector .json en la carpeta [registros_json/] que vincula:
     * - El archivo de la canción en [canciones/]
     * - La carátula en WebP en [webp/]
     * - El archivo de metadatos en [metadatos/]
     */
    fun saveJsonRecord(record: AudioTrackRecord): File {
        val jsonFile = File(jsonRecordsDir, "registro_${record.trackUuid}.json")
        val json = JSONObject().apply {
            put("uuid", record.trackUuid)
            put("titulo", record.title)
            put("artista", record.artist)
            put("album", record.album)
            put("ruta_cancion", record.songFilePath)
            put("ruta_webp_caratula", record.webpCoverPath ?: "")
            put("ruta_metadatos_txt", record.metadataFilePath)
            put("duracion_ms", record.durationMs)
            put("peso_bytes", record.fileSize)
            put("mime_type", record.mimeType)
            put("timestamp_creacion", record.timestamp)
        }
        jsonFile.writeText(json.toString(4), Charsets.UTF_8)
        Log.d(TAG, "Registro conector JSON guardado con éxito en: ${jsonFile.absolutePath}")
        return jsonFile
    }

    /**
     * Genera un nuevo archivo de destino para una canción dentro de la carpeta [canciones/].
     */
    fun createSongFile(extension: String, trackUuid: String = UUID.randomUUID().toString().take(8)): Pair<String, File> {
        val safeExt = if (extension.startsWith(".")) extension.substring(1) else extension
        val fileName = "pista_${trackUuid}.$safeExt"
        val file = File(songsDir, fileName)
        return Pair(trackUuid, file)
    }

    /**
     * Genera el archivo de destino para una carátula WebP dentro de la carpeta [webp/].
     */
    fun createWebpFile(trackUuid: String): File {
        return File(webpDir, "caratula_${trackUuid}.webp")
    }

    /**
     * Lee y parsea todos los registros .json existentes en la carpeta [registros_json/].
     * Permite reconstruir o sincronizar la biblioteca de audio si fuera necesario.
     */
    fun getAllJsonRecords(): List<AudioTrackRecord> {
        val records = mutableListOf<AudioTrackRecord>()
        val files = jsonRecordsDir.listFiles { f -> f.extension.equals("json", ignoreCase = true) } ?: return emptyList()

        for (file in files) {
            try {
                val content = file.readText(Charsets.UTF_8)
                val json = JSONObject(content)
                val record = AudioTrackRecord(
                    trackUuid = json.optString("uuid", file.nameWithoutExtension),
                    title = json.optString("titulo", "Sin título"),
                    artist = json.optString("artista", "Artista desconocido"),
                    album = json.optString("album", "Álbum desconocido"),
                    songFilePath = json.optString("ruta_cancion", ""),
                    webpCoverPath = json.optString("ruta_webp_caratula").takeIf { it.isNotBlank() },
                    metadataFilePath = json.optString("ruta_metadatos_txt", ""),
                    jsonRecordPath = file.absolutePath,
                    durationMs = json.optLong("duracion_ms", 0L),
                    fileSize = json.optLong("peso_bytes", 0L),
                    mimeType = json.optString("mime_type", "audio/mpeg"),
                    timestamp = json.optLong("timestamp_creacion", file.lastModified())
                )
                records.add(record)
            } catch (e: Exception) {
                Log.e(TAG, "Error al leer registro JSON ${file.name}: ${e.message}")
            }
        }
        return records
    }

    /**
     * Elimina todos los archivos asociados a una pista en las 4 carpetas:
     * canción, carátula webp, archivo de texto de metadatos y registro json.
     */
    fun deleteTrackFiles(songPath: String?, webpPath: String?, trackUuid: String?) {
        try {
            // 1. Eliminar canción
            if (!songPath.isNullOrBlank()) {
                val f = File(songPath)
                if (f.exists()) f.delete()
            }
            // 2. Eliminar carátula webp
            if (!webpPath.isNullOrBlank()) {
                val f = File(webpPath)
                if (f.exists()) f.delete()
            }
            // 3. Eliminar metadatos y json por UUID
            if (!trackUuid.isNullOrBlank()) {
                val metaFile = File(metadataDir, "meta_${trackUuid}.txt")
                if (metaFile.exists()) metaFile.delete()

                val jsonFile = File(jsonRecordsDir, "registro_${trackUuid}.json")
                if (jsonFile.exists()) jsonFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar archivos de pista: ${e.message}")
        }
    }
}
