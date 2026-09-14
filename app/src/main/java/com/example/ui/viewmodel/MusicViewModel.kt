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
import com.example.player.equalizer.EqualizerPreset
import com.example.player.equalizer.EqualizerState
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
    EQUALIZER("Ecualizador"),
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
    val equalizerState: StateFlow<EqualizerState> = playbackManager.equalizerManager.equalizerState

    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedTracks: StateFlow<List<TrackEntity>> = repository.recentlyAddedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedTracks: StateFlow<List<TrackEntity>> = repository.mostPlayedTracks
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

    private val _trackToEdit = MutableStateFlow<TrackEntity?>(null)
    val trackToEdit: StateFlow<TrackEntity?> = _trackToEdit.asStateFlow()

    init {
        // Sincronizar el track activo si su información o estado cambia en la base de datos local Room
        viewModelScope.launch {
            repository.allTracks.collect { tracksList ->
                val current = playbackState.value.currentTrack
                if (current != null) {
                    val updatedInDb = tracksList.find { it.id == current.id }
                    if (updatedInDb != null && updatedInDb != current) {
                        playbackManager.updateTrackMetadata(updatedInDb)
                    }
                }
            }
        }
    }

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
            val importedTracks = repository.importTracksFromUris(uris)
            _isImporting.value = false
            if (importedTracks.isNotEmpty()) {
                val count = importedTracks.size
                _userMessage.value = if (count == 1) {
                    "Canción importada: \"${importedTracks.first().title}\""
                } else {
                    "$count canciones importadas con éxito"
                }
                // Reproducción automática de la canción importada inmediatamente
                val targetTrack = importedTracks.first()
                val currentList = allTracks.value
                val combinedQueue = if (currentList.none { it.id == targetTrack.id }) {
                    importedTracks + currentList
                } else {
                    currentList
                }
                playTrack(targetTrack, combinedQueue)
            } else {
                _userMessage.value = "No se pudieron importar los archivos seleccionados"
            }
        }
    }

    fun seedDemoTracks() {
        viewModelScope.launch {
            _isImporting.value = true
            val seeded = repository.seedSampleTracksIfEmpty()
            _isImporting.value = false
            if (seeded.isNotEmpty()) {
                _userMessage.value = "${seeded.size} temas de demostración agregados"
                playTrack(seeded.first(), seeded)
            } else {
                _userMessage.value = "La biblioteca ya contiene temas"
            }
        }
    }

    fun playTrack(track: TrackEntity, customQueue: List<TrackEntity>? = null) {
        val queue = customQueue ?: allTracks.value
        playbackManager.playTrack(track, queue)
        viewModelScope.launch {
            repository.incrementPlayCount(track.id)
        }
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

    fun setGaplessEnabled(enabled: Boolean) = playbackManager.setGaplessEnabled(enabled)

    fun toggleGapless() = playbackManager.toggleGapless()

    fun setTrackToEdit(track: TrackEntity?) {
        _trackToEdit.value = track
    }

    /**
     * Alterna el estado de favorito de una canción.
     * Actualiza al instante el reproductor (mini y pantalla completa) para que el corazón
     * cambie inmediatamente de color y persiste el estado en la base de datos Room.
     */
    fun toggleFavorite(track: TrackEntity) {
        val newStatus = !track.isFavorite
        // Actualización inmediata para retroalimentación visual reactiva sin retardo
        playbackManager.updateTrackFavorite(track.id, newStatus)
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
            _userMessage.value = if (newStatus) "Añadida a favoritos" else "Eliminada de favoritos"
        }
    }

    /**
     * Edita y persiste los metadatos de una pista (título, artista, álbum).
     * Sincroniza la información tanto en Room como en el reproductor activo.
     */
    fun updateTrackMetadata(track: TrackEntity, newTitle: String, newArtist: String, newAlbum: String) {
        if (newTitle.isBlank()) {
            _userMessage.value = "El título de la pista no puede estar vacío"
            return
        }
        viewModelScope.launch {
            val updated = repository.updateTrackMetadata(track.id, newTitle, newArtist, newAlbum)
            if (updated != null) {
                playbackManager.updateTrackMetadata(updated)
                _userMessage.value = "Metadatos actualizados con éxito"
            } else {
                _userMessage.value = "No se pudieron actualizar los metadatos"
            }
            _trackToEdit.value = null
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

    fun setEqualizerEnabled(enabled: Boolean) {
        playbackManager.equalizerManager.setEnabled(enabled)
    }

    fun setEqualizerBandGain(bandIndex: Int, gainDb: Float) {
        playbackManager.equalizerManager.setBandGain(bandIndex, gainDb)
    }

    fun applyEqualizerPreset(preset: EqualizerPreset) {
        playbackManager.equalizerManager.applyPreset(preset)
    }

    fun setEqualizerPreamp(preampDb: Float) {
        playbackManager.equalizerManager.setPreamp(preampDb)
    }

    fun setEqualizerBassBoost(bassBoostDb: Float) {
        playbackManager.equalizerManager.setBassBoost(bassBoostDb)
    }

    fun setEqualizerSoftClip(enabled: Boolean) {
        playbackManager.equalizerManager.setSoftClipEnabled(enabled)
    }

    fun resetEqualizer() {
        playbackManager.equalizerManager.resetToFlat()
        _userMessage.value = "Ecualizador restablecido a curva plana"
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
