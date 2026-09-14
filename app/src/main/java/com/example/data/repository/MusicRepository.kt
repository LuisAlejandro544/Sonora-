package com.example.data.repository

import android.net.Uri
import com.example.data.importer.AudioImporter
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.SonoraDao
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repositorio central de datos de Sonora.
 * Aplica el patrón Repository para abstraer las operaciones de base de datos
 * y almacenamiento de archivos de audio.
 */
class MusicRepository(
    private val dao: SonoraDao,
    private val importer: AudioImporter
) {

    val allTracks: Flow<List<TrackEntity>> = dao.getAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = dao.getFavoriteTracks()
    val recentlyAddedTracks: Flow<List<TrackEntity>> = dao.getRecentlyAddedTracks(12)
    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()
    val totalTrackCount: Flow<Int> = dao.getTotalTrackCount()
    val totalStorageBytes: Flow<Long?> = dao.getTotalStorageBytes()

    fun searchTracks(query: String): Flow<List<TrackEntity>> = dao.searchTracks(query)

    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> =
        dao.getTracksForPlaylist(playlistId)

    suspend fun toggleFavorite(trackId: Long, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(trackId, !currentStatus)
    }

    suspend fun incrementPlayCount(trackId: Long) = withContext(Dispatchers.IO) {
        dao.incrementPlayCount(trackId)
    }

    suspend fun importTracksFromUris(uris: List<Uri>): Int {
        return importer.importAudioUris(uris)
    }

    suspend fun seedSampleTracksIfEmpty(): Int {
        return importer.createSampleTracksIfEmpty()
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(PlaylistEntity(name = name, description = description))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun deleteTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        // Eliminar archivos físicos en las 4 carpetas (canciones, webp, metadatos, json)
        importer.storageManager.deleteTrackFiles(
            songPath = track.filePath,
            webpPath = track.albumArtPath,
            trackUuid = File(track.filePath).nameWithoutExtension.substringAfterLast("_", "")
        )

        // Eliminar de base de datos Room
        dao.deleteTrack(track)
    }
}
