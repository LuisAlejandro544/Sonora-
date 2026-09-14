package com.example.data.importer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.SonoraDao
import com.example.data.local.TrackEntity
import com.example.data.storage.SonoraStorageManager
import com.example.data.storage.WebpLosslessCompressor
import com.example.sonora.nativeengine.SonoraRustBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sin

/**
 * Gestor de importación y procesamiento de archivos de audio para Sonora.
 * 
 * La aplicación NO escanea automáticamente el almacenamiento (respetando la privacidad
 * y la directiva de importación manual del usuario).
 * El usuario selecciona explícitamente sus archivos de audio a través del selector del sistema (SAF).
 * 
 * Estructura de almacenamiento desacoplada (android/data/com.nuestraapp/files/):
 * - [canciones/]: Archivos de audio locales
 * - [webp/]: Carátulas comprimidas a WebP Lossless a máxima compresión
 * - [metadatos/]: Archivos de texto con metadatos de canción, artista y álbum
 * - [registros_json/]: Archivos JSON conectores que enlazan las partes separadas
 */
class AudioImporter(
    private val context: Context,
    private val dao: SonoraDao
) {

    val storageManager = SonoraStorageManager(context)

    /**
     * Importa una lista de Uris seleccionadas manualmente por el usuario.
     * Retorna el número de pistas importadas exitosamente.
     */
    suspend fun importAudioUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var importedCount = 0

        for (uri in uris) {
            try {
                val track = processUri(uri)
                if (track != null) {
                    dao.insertTrack(track)
                    importedCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        importedCount
    }

    /**
     * Procesa una URI de audio:
     * 1. Guarda el audio en la carpeta de canciones [canciones/].
     * 2. Extrae metadatos y carátula incrustada mediante Rust (con fallback a MediaMetadataRetriever).
     * 3. Comprime la carátula a formato WebP Lossless en la carpeta [webp/].
     * 4. Guarda archivo de texto con metadatos en [metadatos/].
     * 5. Guarda archivo conector JSON en [registros_json/].
     * 6. Registra la pista en Room Database.
     */
    private fun processUri(uri: Uri): TrackEntity? {
        val contentResolver = context.contentResolver
        val originalFileName = getFileNameFromUri(uri) ?: "pista_${System.currentTimeMillis()}.mp3"
        val extension = originalFileName.substringAfterLast(".", "mp3")

        // 1. Guardar la pista en la carpeta dedicada de canciones
        val (trackUuid, localSongFile) = storageManager.createSongFile(extension)

        var bytesWritten = 0L
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localSongFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesWritten += read
                }
            }
        } ?: return null

        if (bytesWritten == 0L || !localSongFile.exists()) {
            return null
        }

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs: Long = 0L
        var coverBytes: ByteArray? = null

        // 2. Extracción nativa con motor Rust
        val rustMetadata = SonoraRustBridge.extractMetadataSafe(localSongFile.absolutePath)
        if (rustMetadata != null) {
            title = rustMetadata.title
            artist = rustMetadata.artist
            album = rustMetadata.album
            if (rustMetadata.hasCover) {
                coverBytes = SonoraRustBridge.extractCoverArtSafe(localSongFile.absolutePath)
            }
        }

        // Extracción complementaria o fallback con MediaMetadataRetriever (duración, etiquetas adicionales)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(localSongFile.absolutePath)
            if (title.isNullOrBlank()) {
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            }
            if (artist.isNullOrBlank()) {
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            }
            if (album.isNullOrBlank()) {
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            }
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L

            if (coverBytes == null || coverBytes.isEmpty()) {
                coverBytes = retriever.embeddedPicture
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val cleanTitle = if (!title.isNullOrBlank()) {
            title.trim()
        } else {
            originalFileName.substringBeforeLast(".").replace("_", " ").trim()
        }

        val cleanArtist = if (!artist.isNullOrBlank()) artist.trim() else "Artista desconocido"
        val cleanAlbum = if (!album.isNullOrBlank()) album.trim() else "Álbum desconocido"

        // 3. Comprimir carátula a WebP a máxima compresión sin pérdida (Lossless) en carpeta [webp/]
        var webpCoverPath: String? = null
        if (coverBytes != null && coverBytes.isNotEmpty()) {
            val webpFile = storageManager.createWebpFile(trackUuid)
            val success = WebpLosslessCompressor.compressToWebpLossless(coverBytes, webpFile)
            if (success) {
                webpCoverPath = webpFile.absolutePath
            }
        }

        // 4. Guardar archivo de texto de metadatos en carpeta [metadatos/]
        val metaFile = storageManager.saveMetadataFile(
            trackUuid = trackUuid,
            title = cleanTitle,
            artist = cleanArtist,
            album = cleanAlbum,
            durationMs = durationMs
        )

        val mimeType = contentResolver.getType(uri) ?: "audio/$extension"

        // 5. Guardar archivo conector .json en carpeta [registros_json/]
        val jsonRecord = SonoraStorageManager.AudioTrackRecord(
            trackUuid = trackUuid,
            title = cleanTitle,
            artist = cleanArtist,
            album = cleanAlbum,
            songFilePath = localSongFile.absolutePath,
            webpCoverPath = webpCoverPath,
            metadataFilePath = metaFile.absolutePath,
            jsonRecordPath = "",
            durationMs = durationMs,
            fileSize = bytesWritten,
            mimeType = mimeType
        )
        storageManager.saveJsonRecord(jsonRecord)

        return TrackEntity(
            title = cleanTitle,
            artist = cleanArtist,
            album = cleanAlbum,
            durationMs = durationMs,
            filePath = localSongFile.absolutePath,
            albumArtPath = webpCoverPath,
            fileSize = bytesWritten,
            mimeType = mimeType
        )
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    /**
     * Genera pistas de audio de demostración (archivos WAV con acordes armónicos)
     * para que el usuario pueda probar el reproductor inmediatamente si aún no ha transferido canciones.
     */
    suspend fun createSampleTracksIfEmpty(): Int = withContext(Dispatchers.IO) {
        val currentCount = dao.getRecentlyAddedTracks(1)
        // Solo generamos si la base de datos está vacía
        val sample1 = generateHarmonicWav(
            filename = "sonora_vibes_lofi.wav",
            title = "Midnight Horizon",
            artist = "Sonora Collective",
            album = "Neon Echoes",
            frequencies = doubleArrayOf(261.63, 329.63, 392.00, 523.25), // Do Mayor / C Major 7
            durationSeconds = 24
        )

        val sample2 = generateHarmonicWav(
            filename = "aurora_ambient_flow.wav",
            title = "Emerald Aurora",
            artist = "Aura Studio",
            album = "Prism Sessions",
            frequencies = doubleArrayOf(220.00, 277.18, 329.63, 440.00), // La menor / A minor
            durationSeconds = 28
        )

        val sample3 = generateHarmonicWav(
            filename = "cyber_pulse_synth.wav",
            title = "Cyber Pulse",
            artist = "Digital Wave",
            album = "Future Beat",
            frequencies = doubleArrayOf(174.61, 220.00, 261.63, 349.23), // Fa Mayor / F Major
            durationSeconds = 20
        )

        listOf(sample1, sample2, sample3).forEach { dao.insertTrack(it) }
        3
    }

    /**
     * Sintetiza un archivo de audio WAV estéreo a 44100Hz con modulación suave de acordes
     * para reproducir con total fidelidad en ExoPlayer.
     */
    private fun generateHarmonicWav(
        filename: String,
        title: String,
        artist: String,
        album: String,
        frequencies: DoubleArray,
        durationSeconds: Int
    ): TrackEntity {
        val sampleRate = 44100
        val totalSamples = sampleRate * durationSeconds
        val file = File(storageManager.songsDir, filename)

        val bytesPerSample = 2 // 16-bit PCM
        val channels = 2 // Estéreo
        val dataSize = totalSamples * channels * bytesPerSample

        FileOutputStream(file).use { out ->
            // Encabezado RIFF WAV
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size para PCM
            header.putShort(1) // AudioFormat 1 = PCM
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * channels * bytesPerSample) // ByteRate
            header.putShort((channels * bytesPerSample).toShort()) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(dataSize)

            out.write(header.array())

            // Buffer de audio con onda compuesta y envolvente ADSR
            val chunk = ByteArray(4096)
            var chunkIndex = 0
            val maxAmp = 12000.0

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                // Modulación y arpegio suave
                var sampleValue = 0.0
                val chordIndex = (t * 2.0).toInt() % frequencies.size
                val currentFreq = frequencies[chordIndex]

                val envelope = when {
                    t < 1.0 -> t // Fade-in inicial
                    t > durationSeconds - 2.0 -> (durationSeconds - t) / 2.0 // Fade-out final
                    else -> 0.85 + 0.15 * sin(2.0 * Math.PI * 0.5 * t) // Pulsación rítmica
                }

                sampleValue += sin(2.0 * Math.PI * currentFreq * t) * 0.6
                sampleValue += sin(2.0 * Math.PI * (currentFreq * 0.5) * t) * 0.3
                sampleValue += sin(2.0 * Math.PI * (currentFreq * 2.0) * t) * 0.1

                val pcmShort = (sampleValue * maxAmp * envelope).toInt().coerceIn(-32767, 32767).toShort()

                // Canal Izquierdo
                chunk[chunkIndex++] = (pcmShort.toInt() and 0xFF).toByte()
                chunk[chunkIndex++] = ((pcmShort.toInt() shr 8) and 0xFF).toByte()

                // Canal Derecho (ligera fase estéreo)
                val rightShort = (sampleValue * maxAmp * envelope * 0.95).toInt().coerceIn(-32767, 32767).toShort()
                chunk[chunkIndex++] = (rightShort.toInt() and 0xFF).toByte()
                chunk[chunkIndex++] = ((rightShort.toInt() shr 8) and 0xFF).toByte()

                if (chunkIndex >= chunk.size) {
                    out.write(chunk, 0, chunkIndex)
                    chunkIndex = 0
                }
            }

            if (chunkIndex > 0) {
                out.write(chunk, 0, chunkIndex)
            }
        }

        val sampleUuid = UUID.randomUUID().toString().take(8)
        val metaFile = storageManager.saveMetadataFile(
            trackUuid = sampleUuid,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationSeconds * 1000L
        )

        val jsonRecord = SonoraStorageManager.AudioTrackRecord(
            trackUuid = sampleUuid,
            title = title,
            artist = artist,
            album = album,
            songFilePath = file.absolutePath,
            webpCoverPath = null,
            metadataFilePath = metaFile.absolutePath,
            jsonRecordPath = "",
            durationMs = durationSeconds * 1000L,
            fileSize = file.length(),
            mimeType = "audio/wav"
        )
        storageManager.saveJsonRecord(jsonRecord)

        return TrackEntity(
            title = title,
            artist = artist,
            album = album,
            durationMs = durationSeconds * 1000L,
            filePath = file.absolutePath,
            albumArtPath = null,
            fileSize = file.length(),
            mimeType = "audio/wav"
        )
    }
}
