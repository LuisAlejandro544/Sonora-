package com.example.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.data.local.TrackEntity
import com.example.player.equalizer.EqualizerManager
import com.example.player.equalizer.Sonora10BandAudioProcessor
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
 * Implementa el patrón Singleton para coordinar la reproducción entre la interfaz de usuario
 * (Jetpack Compose / ViewModel) y el servicio en segundo plano (SonoraMediaService).
 *
 * Expone un MediaSession para la barra de notificaciones del sistema con controles
 * multimedia interactivos y carátula del álbum en alta resolución.
 */
class PlaybackManager private constructor(
    private val context: Context
) {
    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var onTrackFinished: ((TrackEntity) -> Unit)? = null

    fun setTrackFinishedListener(listener: (TrackEntity) -> Unit) {
        this.onTrackFinished = listener
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val equalizerManager: EqualizerManager = EqualizerManager.getInstance(context)
    val vocalEngineManager: com.example.player.vocal.VocalEngineManager = com.example.player.vocal.VocalEngineManager.getInstance(context)

    private val audioProcessor = Sonora10BandAudioProcessor(equalizerManager)
    private val vocalAudioProcessor = com.example.player.vocal.SonoraVocalAudioProcessor(vocalEngineManager)

    private val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(audioProcessor, vocalAudioProcessor))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        // Control de búfer optimizado para Reproducción Sin Pausas (Gapless Playback).
        // Precarga anticipada de la siguiente pista para eliminar micro-silencios y latencia entre canciones contiguas.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(20_000, false)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build()
                setAudioAttributes(audioAttributes, true)
                // pauseAtEndOfMediaItems = false garantiza transición continua (gapless) entre pistas
                pauseAtEndOfMediaItems = false
                addListener(playerListener)
            }
    }

    /**
     * Sesión MediaSession de Media3 vinculada a ExoPlayer.
     * Permite a la System UI de Android (pantalla de bloqueo y panel de notificaciones)
     * mostrar el mini reproductor con carátula y botones de control.
     */
    val mediaSession: MediaSession by lazy {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        MediaSession.Builder(context, exoPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    /**
     * Inicia el servicio en primer plano SonoraMediaService si aún no está en ejecución.
     */
    private fun ensureServiceRunning() {
        try {
            val serviceIntent = Intent(context, SonoraMediaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // Manejo preventivo si existen restricciones de inicio en background
        }
    }

    fun isPlaying(): Boolean = exoPlayer.isPlaying

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressUpdates()
                ensureServiceRunning()
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

            // Extraer bytes de carátula local (WebP Lossless o Procedural)
            val artworkBytes = item.albumArtPath?.let { path ->
                try {
                    val artFile = File(path)
                    if (artFile.exists() && artFile.length() > 0) {
                        artFile.readBytes()
                    } else null
                } catch (_: Exception) {
                    null
                }
            }

            val artworkUri = item.albumArtPath?.let { path ->
                val artFile = File(path)
                if (artFile.exists()) Uri.fromFile(artFile) else null
            }

            val meta = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setAlbumTitle(item.album)
                .apply {
                    if (artworkBytes != null) {
                        setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                    if (artworkUri != null) {
                        setArtworkUri(artworkUri)
                    }
                }
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
        ensureServiceRunning()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _playbackState.value.currentTrack != null) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
            ensureServiceRunning()
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

    /**
     * Activa o desactiva la Reproducción Sin Pausas (Gapless Playback).
     * Cuando está activada, ExoPlayer precarga y encadena las pistas contiguas sin silencios.
     */
    fun setGaplessEnabled(enabled: Boolean) {
        exoPlayer.pauseAtEndOfMediaItems = !enabled
        _playbackState.update { it.copy(isGaplessEnabled = enabled) }
    }

    /**
     * Alterna el estado de Reproducción Sin Pausas (Gapless Playback).
     */
    fun toggleGapless() {
        setGaplessEnabled(!_playbackState.value.isGaplessEnabled)
    }

    /**
     * Sincroniza en tiempo real el estado de favorito de una pista en la cola y en el reproductor activo.
     * Permite feedback visual instantáneo en el mini reproductor y reproductor completo.
     */
    fun updateTrackFavorite(trackId: Long, isFavorite: Boolean) {
        _playbackState.update { state ->
            val updatedQueue = state.queue.map { track ->
                if (track.id == trackId) track.copy(isFavorite = isFavorite) else track
            }
            val updatedCurrent = if (state.currentTrack?.id == trackId) {
                state.currentTrack.copy(isFavorite = isFavorite)
            } else {
                state.currentTrack
            }
            state.copy(
                queue = updatedQueue,
                currentTrack = updatedCurrent
            )
        }
    }

    /**
     * Sincroniza metadatos editados (título, artista, álbum) en la pista activa y la cola en memoria.
     */
    fun updateTrackMetadata(updatedTrack: TrackEntity) {
        _playbackState.update { state ->
            val updatedQueue = state.queue.map { track ->
                if (track.id == updatedTrack.id) updatedTrack else track
            }
            val updatedCurrent = if (state.currentTrack?.id == updatedTrack.id) {
                updatedTrack
            } else {
                state.currentTrack
            }
            state.copy(
                queue = updatedQueue,
                currentTrack = updatedCurrent
            )
        }
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
