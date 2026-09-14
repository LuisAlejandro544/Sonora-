package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlaybackState
import com.example.player.SonoraRepeatMode
import com.example.ui.components.AudioVisualizerBars
import com.example.ui.components.Semi3DCard
import com.example.ui.components.SonoraAlbumArt
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraHeartActive
import com.example.ui.theme.SonoraProgressBackground
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Pantalla completa del reproductor de música Sonora.
 * Inspirada en la interfaz inmersiva de streaming, con control táctil semi-3D,
 * scrubber interactivo, ecualizador en vivo, selector de velocidad y carátula grande sin rotación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    isOpen: Boolean,
    playbackState: PlaybackState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val track = playbackState.currentTrack

    AnimatedVisibility(
        visible = isOpen && track != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        if (track == null) return@AnimatedVisibility

        var isDraggingSlider by remember { mutableStateOf(false) }
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var showSpeedDialog by remember { mutableStateOf(false) }
        var showInfoDialog by remember { mutableStateOf(false) }

        val currentPosition = if (isDraggingSlider) {
            (sliderPosition * track.durationMs).toLong()
        } else {
            playbackState.currentPositionMs
        }

        // Fondo con sutil gradiente ambiental que desciende desde verde carbón profundo a negro puro
        val backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                SonoraEmeraldDark.copy(alpha = 0.45f),
                SonoraBackground,
                SonoraBackground
            )
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .testTag("full_screen_player_view"),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Barra superior: Cerrar y Título del contexto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_player_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimizar reproductor",
                            tint = SonoraTextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "REPRODUCIENDO LOCAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SonoraTextSecondary
                        )
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = SonoraEmeraldBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información del audio",
                            tint = SonoraTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Carátula del álbum con sombra semi-3D envolvente y bordes biselados (Sin rotación)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    SonoraAlbumArt(
                        albumArtPath = track.albumArtPath,
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color.Black.copy(alpha = 0.8f),
                                spotColor = SonoraEmerald.copy(alpha = 0.35f)
                            ),
                        cornerRadius = 20.dp,
                        elevation = 12.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Título de la pista, artista y botón de Me gusta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = SonoraTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp
                            ),
                            color = SonoraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botón de Corazón (Favorito)
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (track.isFavorite) "Quitar de favoritos" else "Guardar en favoritos",
                            tint = if (track.isFavorite) SonoraHeartActive else SonoraTextSecondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barra de progreso y tiempos
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = if (track.durationMs > 0L) {
                            if (isDraggingSlider) sliderPosition else (currentPosition.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f,
                        onValueChange = { value ->
                            isDraggingSlider = true
                            sliderPosition = value
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            val targetMs = (sliderPosition * track.durationMs).toLong()
                            onSeekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = SonoraEmeraldBright,
                            activeTrackColor = SonoraEmerald,
                            inactiveTrackColor = SonoraProgressBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playback_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SonoraTextMuted
                        )
                        Text(
                            text = formatDuration(track.durationMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SonoraTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Controles de reproducción con botones táctiles semi-3D
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Aleatorio (Shuffle)
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Modo aleatorio",
                            tint = if (playbackState.isShuffleEnabled) SonoraEmeraldBright else SonoraTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Botón Anterior
                    IconButton(
                        onClick = onPlayPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_previous_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Pista anterior",
                            tint = SonoraTextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Botón Principal Play/Pause: Gran círculo con relieve semi-3D y resplandor
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                spotColor = SonoraEmeraldBright.copy(alpha = 0.6f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(SonoraEmeraldBright, SonoraEmerald)
                                )
                            )
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("player_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Botón Siguiente
                    IconButton(
                        onClick = onPlayNext,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Pista siguiente",
                            tint = SonoraTextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Botón Repetición (Cycle Repeat: OFF, ALL, ONE)
                    IconButton(
                        onClick = onCycleRepeatMode,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_repeat_btn")
                    ) {
                        val (icon, tint) = when (playbackState.repeatMode) {
                            SonoraRepeatMode.OFF -> Pair(Icons.Filled.Repeat, SonoraTextMuted)
                            SonoraRepeatMode.ALL -> Pair(Icons.Filled.Repeat, SonoraEmeraldBright)
                            SonoraRepeatMode.ONE -> Pair(Icons.Filled.RepeatOne, SonoraEmeraldBright)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Modo de repetición",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Fila inferior: Visualizador sutil y control de velocidad
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visualizador de audio rítmico en vivo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        AudioVisualizerBars(
                            isPlaying = playbackState.isPlaying,
                            modifier = Modifier
                                .width(40.dp)
                                .height(20.dp),
                            barColor = if (playbackState.isPlaying) SonoraEmeraldBright else SonoraTextMuted,
                            barCount = 5
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (playbackState.isPlaying) "En reproducción" else "En pausa",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonoraTextSecondary
                        )
                    }

                    // Botón de Velocidad de reproducción (0.75x, 1.0x, etc.)
                    Semi3DCard(
                        elevation = 2.dp,
                        cornerRadius = 20.dp,
                        onClick = { showSpeedDialog = true },
                        modifier = Modifier.testTag("speed_selector_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Velocidad de reproducción",
                                tint = SonoraEmeraldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playbackState.playbackSpeed}x",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SonoraTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Diálogo de selección de velocidad
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = {
                    Text(
                        "Velocidad de Reproducción",
                        color = SonoraTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        speeds.forEach { spd ->
                            val isSelected = playbackState.playbackSpeed == spd
                            Button(
                                onClick = {
                                    onSetPlaybackSpeed(spd)
                                    showSpeedDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) SonoraEmerald else SonoraSurfaceHighlight
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x",
                                    color = if (isSelected) Color.Black else SonoraTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Cerrar", color = SonoraEmeraldBright)
                    }
                },
                containerColor = SonoraSurfaceElevated
            )
        }

        // Diálogo de detalles técnicos de la pista
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Text(
                        "Detalles del Archivo",
                        color = SonoraTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow(label = "Título", value = track.title)
                        InfoRow(label = "Artista", value = track.artist)
                        InfoRow(label = "Álbum", value = track.album)
                        InfoRow(label = "Duración", value = formatDuration(track.durationMs))
                        InfoRow(label = "Tamaño", value = formatFileSize(track.fileSize))
                        InfoRow(label = "Formato MIME", value = track.mimeType ?: "Desconocido")
                        InfoRow(label = "Reproducciones", value = "${track.playCount}")
                        InfoRow(label = "Ruta local", value = track.filePath)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Aceptar", color = SonoraEmeraldBright)
                    }
                },
                containerColor = SonoraSurfaceElevated
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = SonoraEmeraldBright
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = SonoraTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
