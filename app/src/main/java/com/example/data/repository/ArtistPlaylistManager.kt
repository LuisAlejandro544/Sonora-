package com.example.data.repository

import android.util.Log
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.SonoraDao
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ==============================================================================
 * GESTOR DE PLAYLISTS AUTOMÁTICAS POR ARTISTA (ArtistPlaylistManager.kt)
 * ==============================================================================
 * Supervisa la biblioteca de Sonora y gestiona listas automáticas:
 * 1. Si detecta 3 o más canciones de un mismo artista, crea automáticamente una playlist
 *    dedicada para ese artista (ej: "Queen", "Daft Punk").
 * 2. Si con posterioridad se importan o agregan nuevas canciones del mismo artista,
 *    las incorpora automáticamente a su playlist correspondiente sin duplicar.
 * ==============================================================================
 */
class ArtistPlaylistManager(private val dao: SonoraDao) {

    companion object {
        private const val TAG = "ArtistPlaylistManager"
        const val MIN_TRACKS_FOR_AUTO_PLAYLIST = 3
        const val AUTO_PLAYLIST_DESC_PREFIX = "Colección automática • "

        // Lista de nombres genéricos a ignorar para la generación automática
        private val IGNORED_ARTISTS = setOf(
            "desconocido",
            "artista desconocido",
            "unknown",
            "unknown artist",
            "<unknown>",
            "various artists",
            "varios artistas"
        )
    }

    /**
     * Sincroniza y actualiza las listas automáticas de artistas basándose en la lista
     * de pistas [allTracks].
     * 
     * @return Número de listas creadas o actualizadas con nuevos temas.
     */
    suspend fun syncArtistPlaylists(allTracks: List<TrackEntity>): Int = withContext(Dispatchers.IO) {
        if (allTracks.isEmpty()) return@withContext 0

        // 1. Filtrar pistas con artista válido
        val validTracks = allTracks.filter { track ->
            val artistClean = track.artist.trim().lowercase()
            artistClean.isNotBlank() && !IGNORED_ARTISTS.contains(artistClean)
        }

        // 2. Agrupar por artista (usando clave en minúsculas para consistencia, conservando capitalización representativa)
        val groupedByArtist = validTracks.groupBy { it.artist.trim().lowercase() }

        var playlistsUpdatedOrCreated = 0

        for ((_, tracksForArtist) in groupedByArtist) {
            // Regla: Requiere un mínimo de 3 canciones del mismo artista
            if (tracksForArtist.size >= MIN_TRACKS_FOR_AUTO_PLAYLIST) {
                // Tomar el nombre del artista con la mejor capitalización
                val displayArtistName = tracksForArtist
                    .map { it.artist.trim() }
                    .maxByOrNull { it.count { c -> c.isUpperCase() } } ?: tracksForArtist.first().artist.trim()

                // Buscar si ya existe una lista para este artista
                val existingPlaylist = dao.getPlaylistByName(displayArtistName)

                if (existingPlaylist == null) {
                    // Crear nueva playlist automática para el artista
                    val newPlaylistId = dao.insertPlaylist(
                        PlaylistEntity(
                            name = displayArtistName,
                            description = "$AUTO_PLAYLIST_DESC_PREFIX$displayArtistName"
                        )
                    )
                    Log.i(TAG, "Creada playlist automática para artista: \"$displayArtistName\" con ${tracksForArtist.size} canciones")

                    // Agregar todas las pistas del artista a la nueva playlist
                    tracksForArtist.forEach { track ->
                        dao.addTrackToPlaylist(PlaylistTrackCrossRef(newPlaylistId, track.id))
                    }
                    playlistsUpdatedOrCreated++
                } else {
                    // La playlist ya existe: verificar si hay nuevas canciones que no hayan sido agregadas
                    val currentTrackIds = dao.getTrackIdsForPlaylist(existingPlaylist.id).toSet()
                    var newlyAddedCount = 0

                    for (track in tracksForArtist) {
                        if (!currentTrackIds.contains(track.id)) {
                            dao.addTrackToPlaylist(PlaylistTrackCrossRef(existingPlaylist.id, track.id))
                            newlyAddedCount++
                        }
                    }

                    if (newlyAddedCount > 0) {
                        Log.i(TAG, "Incorporadas $newlyAddedCount nuevas canciones a la playlist de \"$displayArtistName\"")
                        playlistsUpdatedOrCreated++
                    }
                }
            }
        }

        playlistsUpdatedOrCreated
    }
}
