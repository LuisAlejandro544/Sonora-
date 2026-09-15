package com.example.data.repository

import android.util.Log
import com.example.data.local.TrackEntity
import com.example.sonora.nativeengine.SonoraRustBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ==============================================================================
 * GESTOR DE SANEAMIENTO Y LIMPIEZA DE METADATOS EN RUST (MetadataSanitizerManager.kt)
 * ==============================================================================
 * Coordina las operaciones de limpieza y desinfección de metadatos llamando
 * al motor nativo en Rust (libsonora_rust.so):
 * - Corrige caracteres mojibake y codificaciones corruptas.
 * - Elimina etiquetas basura de ripeo web ([y2mate.com], (320kbps), [Official Audio]).
 * - Separa cadenas "Artista - Canción" fusionadas.
 * - Normaliza títulos y aplica Title Case inteligente.
 * ==============================================================================
 */
class MetadataSanitizerManager(private val repository: MusicRepository) {

    companion object {
        private const val TAG = "MetadataSanitizer"
    }

    /**
     * Resultado devuelto tras un saneamiento individual.
     */
    data class SingleSanitizeResult(
        val originalTrack: TrackEntity,
        val updatedTrack: TrackEntity?,
        val wasModified: Boolean,
        val cleanedTitle: String,
        val cleanedArtist: String,
        val cleanedAlbum: String
    )

    /**
     * Sanea los metadatos de una canción específica utilizando el motor nativo de Rust.
     */
    suspend fun sanitizeTrack(track: TrackEntity): SingleSanitizeResult = withContext(Dispatchers.IO) {
        val rustResult = SonoraRustBridge.sanitizeMetadataSafe(
            title = track.title,
            artist = track.artist,
            album = track.album,
            filePath = track.filePath
        )

        if (rustResult != null && rustResult.wasModified) {
            Log.i(TAG, "Canción saneada por Rust: \"${track.title}\" -> \"${rustResult.title}\" | Artista: \"${rustResult.artist}\"")
            val updated = repository.updateTrackMetadata(
                trackId = track.id,
                newTitle = rustResult.title,
                newArtist = rustResult.artist,
                newAlbum = rustResult.album
            )
            SingleSanitizeResult(
                originalTrack = track,
                updatedTrack = updated,
                wasModified = true,
                cleanedTitle = rustResult.title,
                cleanedArtist = rustResult.artist,
                cleanedAlbum = rustResult.album
            )
        } else {
            SingleSanitizeResult(
                originalTrack = track,
                updatedTrack = null,
                wasModified = false,
                cleanedTitle = rustResult?.title ?: track.title,
                cleanedArtist = rustResult?.artist ?: track.artist,
                cleanedAlbum = rustResult?.album ?: track.album
            )
        }
    }

    /**
     * Sanea en lote todas las canciones de la biblioteca ejecutando el análisis en Rust.
     * Retorna el número de canciones que tenían metadatos corruptos o raros y fueron limpiadas.
     */
    suspend fun sanitizeAllTracks(tracks: List<TrackEntity>): Int = withContext(Dispatchers.IO) {
        var modifiedCount = 0
        for (track in tracks) {
            val result = sanitizeTrack(track)
            if (result.wasModified) {
                modifiedCount++
            }
        }
        modifiedCount
    }
}
