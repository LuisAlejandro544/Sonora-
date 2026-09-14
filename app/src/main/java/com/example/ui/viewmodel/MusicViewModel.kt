package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.importer.AudioImporter
import com.example.data.local.PlaylistEntity
import com.example.data.local.SonoraDatabase
import com.example.data.local.TrackEntity
import com.example.data.repository.MusicRepository
import com.example.player.PlaybackManager
import com.example.player.PlaybackState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Pestañas de navegación principal en la aplicación Sonora.
 */
enum class SonoraNavTab(val title: String) {
    HOME("Inicio"),
    LIBRARY("Biblioteca"),
    PLAYLISTS("Listas"),
    SETTINGS("Ajustes")
}

/**
 * ViewModel principal de Sonora.
 * Coordina la capa de datos Room, la importación de canciones seleccionadas
 * y el motor de audio ExoPlayer / Media3.
 */
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SonoraDatabase.getInstance(application)
    private val importer = AudioImporter(application, database.sonoraDao())
    private val repository = MusicRepository(database.sonoraDao(), importer)

    private val playbackManager = PlaybackManager(application) { finishedTrack ->
        viewModelScope.launch {
            repository.incrementPlayCount(finishedTrack.id)
        }
    }

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedTracks: StateFlow<List<TrackEntity>> = repository.recentlyAddedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTrackCount: StateFlow<Int> = repository.totalTrackCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStorageBytes: StateFlow<Long?> = repository.totalStorageBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<TrackEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchTracks(query)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentNavTab = MutableStateFlow(SonoraNavTab.HOME)
    val currentNavTab: StateFlow<SonoraNavTab> = _currentNavTab.asStateFlow()

    private val _isFullScreenPlayerOpen = MutableStateFlow(false)
    val isFullScreenPlayerOpen: StateFlow<Boolean> = _isFullScreenPlayerOpen.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPlaylistTracks: StateFlow<List<TrackEntity>> = _selectedPlaylist
        .flatMapLatest { playlist ->
            if (playlist != null) {
                repository.getTracksForPlaylist(playlist.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setNavTab(tab: SonoraNavTab) {
        _currentNavTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openFullScreenPlayer() {
        if (playbackState.value.currentTrack != null) {
            _isFullScreenPlayerOpen.value = true
        }
    }

    fun closeFullScreenPlayer() {
        _isFullScreenPlayerOpen.value = false
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            val count = repository.importTracksFromUris(uris)
            _isImporting.value = false
            _userMessage.value = if (count > 0) {
                "$count canciones importadas con éxito"
            } else {
                "No se pudieron importar los archivos seleccionados"
            }
        }
    }

    fun seedDemoTracks() {
        viewModelScope.launch {
            _isImporting.value = true
            val count = repository.seedSampleTracksIfEmpty()
            _isImporting.value = false
            _userMessage.value = "$count temas de demostración agregados"
        }
    }

    fun playTrack(track: TrackEntity, customQueue: List<TrackEntity>? = null) {
        val queue = customQueue ?: allTracks.value
        playbackManager.playTrack(track, queue)
    }

    fun playAll(tracks: List<TrackEntity>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val listToPlay = if (shuffle) tracks.shuffled() else tracks
        playbackManager.playTrack(listToPlay.first(), listToPlay)
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun playNext() = playbackManager.playNext()

    fun playPrevious() = playbackManager.playPrevious()

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun toggleShuffle() = playbackManager.toggleShuffle()

    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()

    fun setPlaybackSpeed(speed: Float) = playbackManager.setPlaybackSpeed(speed)

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
        }
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            repository.deleteTrack(track)
            _userMessage.value = "\"${track.title}\" eliminada"
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            _userMessage.value = "Lista \"$name\" creada"
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
            _userMessage.value = "Lista eliminada"
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            _userMessage.value = "Canción agregada a la lista"
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            _userMessage.value = "Canción removida de la lista"
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
