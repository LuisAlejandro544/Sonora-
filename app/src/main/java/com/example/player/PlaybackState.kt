package com.example.player

import com.example.data.local.TrackEntity

/**
 * Modos de repetición del reproductor.
 */
enum class SonoraRepeatMode {
    OFF,   // Sin repetición
    ALL,   // Repetir toda la cola
    ONE    // Repetir la canción actual
}

/**
 * Estado inmutable de la reproducción multimedia en Sonora.
 */
data class PlaybackState(
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: SonoraRepeatMode = SonoraRepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val queue: List<TrackEntity> = emptyList(),
    val currentIndex: Int = -1,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val isGaplessEnabled: Boolean = true
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
