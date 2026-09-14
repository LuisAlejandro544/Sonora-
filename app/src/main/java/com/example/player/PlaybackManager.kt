package com.example.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Gestor del motor de audio de Sonora utilizando ExoPlayer y Media3.
 * 
 * Controla el ciclo de vida del reproductor, colas de reproducción, modos aleatorio y repetición,
 * cambios de velocidad y emite el estado reactivo mediante StateFlow.
 */
class PlaybackManager(
    private val context: Context,
    private val onTrackFinished: ((TrackEntity) -> Unit)? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build()
                setAudioAttributes(audioAttributes, true)
                addListener(playerListener)
            }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            val isBuffering = state == Player.STATE_BUFFERING
            _playbackState.update { it.copy(isBuffering = isBuffering) }

            if (state == Player.STATE_READY) {
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                _playbackState.update { it.copy(durationMs = dur) }
            } else if (state == Player.STATE_ENDED) {
                _playbackState.value.currentTrack?.let { onTrackFinished?.invoke(it) }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = exoPlayer.currentMediaItemIndex
            val queue = _playbackState.value.queue
            if (index in queue.indices) {
                val current = queue[index]
                _playbackState.update {
                    it.copy(
                        currentTrack = current,
                        currentIndex = index,
                        durationMs = current.durationMs,
                        currentPositionMs = 0L
                    )
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Error al reproducir: ${error.localizedMessage ?: "Archivo no reproducible"}"
                )
            }
        }
    }

    /**
     * Reproduce una pista específica dentro de una lista de canciones.
     */
    fun playTrack(track: TrackEntity, newQueue: List<TrackEntity>) {
        val targetIndex = newQueue.indexOfFirst { it.id == track.id }.let { if (it == -1) 0 else it }

        val mediaItems = newQueue.map { item ->
            val uri = Uri.fromFile(File(item.filePath))
            val meta = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setAlbumTitle(item.album)
                .build()

            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(uri)
                .setMediaMetadata(meta)
                .build()
        }

        _playbackState.update {
            it.copy(
                queue = newQueue,
                currentTrack = track,
                currentIndex = targetIndex,
                durationMs = track.durationMs,
                currentPositionMs = 0L,
                errorMessage = null
            )
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItems(mediaItems, targetIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _playbackState.value.currentTrack != null) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    fun playNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if (_playbackState.value.repeatMode == SonoraRepeatMode.ALL && _playbackState.value.queue.isNotEmpty()) {
            exoPlayer.seekTo(0, 0L)
        }
    }

    fun playPrevious() {
        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else if (_playbackState.value.queue.isNotEmpty()) {
            exoPlayer.seekTo(0, 0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(bounded)
        _playbackState.update { it.copy(currentPositionMs = bounded) }
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffleEnabled
        exoPlayer.shuffleModeEnabled = newShuffle
        _playbackState.update { it.copy(isShuffleEnabled = newShuffle) }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            SonoraRepeatMode.OFF -> SonoraRepeatMode.ALL
            SonoraRepeatMode.ALL -> SonoraRepeatMode.ONE
            SonoraRepeatMode.ONE -> SonoraRepeatMode.OFF
        }

        exoPlayer.repeatMode = when (nextMode) {
            SonoraRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            SonoraRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            SonoraRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }

        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                val duration = exoPlayer.duration.coerceAtLeast(0L)
                _playbackState.update {
                    it.copy(
                        currentPositionMs = current,
                        durationMs = if (duration > 0L) duration else it.durationMs
                    )
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressUpdates()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
