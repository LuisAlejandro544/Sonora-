package com.example.data.repository

import android.net.Uri
import com.example.data.importer.AudioImporter
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.SonoraDao
import com.example.data.local.TrackEntity
import com.example.data.storage.SonoraStorageManager
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
    val mostPlayedTracks: Flow<List<TrackEntity>> = dao.getMostPlayedTracks(10)
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

    suspend fun importTracksFromUris(uris: List<Uri>): List<TrackEntity> {
        return importer.importAudioUris(uris)
    }

    suspend fun seedSampleTracksIfEmpty(): List<TrackEntity> {
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

    /**
     * Actualiza los metadatos de una pista (título, artista, álbum) en la base de datos Room
     * y sincroniza los archivos locales de metadatos de texto y el conector JSON.
     */
    suspend fun updateTrackMetadata(
        trackId: Long,
        newTitle: String,
        newArtist: String,
        newAlbum: String
    ): TrackEntity? = withContext(Dispatchers.IO) {
        val existing = dao.getTrackById(trackId) ?: return@withContext null
        val updated = existing.copy(
            title = newTitle.trim(),
            artist = newArtist.trim().ifEmpty { "Artista desconocido" },
            album = newAlbum.trim().ifEmpty { "Álbum desconocido" }
        )
        dao.updateTrack(updated)

        // Sincronizar archivo físico en [metadatos/] y [registros_json/]
        try {
            val songFile = File(existing.filePath)
            val trackUuid = songFile.nameWithoutExtension.substringAfterLast("_", "")
            if (trackUuid.isNotEmpty()) {
                val metaFile = importer.storageManager.saveMetadataFile(
                    trackUuid = trackUuid,
                    title = updated.title,
                    artist = updated.artist,
                    album = updated.album,
                    durationMs = updated.durationMs
                )
                val jsonRecord = SonoraStorageManager.AudioTrackRecord(
                    trackUuid = trackUuid,
                    title = updated.title,
                    artist = updated.artist,
                    album = updated.album,
                    songFilePath = updated.filePath,
                    webpCoverPath = updated.albumArtPath,
                    metadataFilePath = metaFile.absolutePath,
                    jsonRecordPath = File(importer.storageManager.jsonRecordsDir, "record_${trackUuid}.json").absolutePath,
                    durationMs = updated.durationMs,
                    fileSize = updated.fileSize,
                    mimeType = updated.mimeType ?: "audio/mpeg"
                )
                importer.storageManager.saveJsonRecord(jsonRecord)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updated
    }
}
