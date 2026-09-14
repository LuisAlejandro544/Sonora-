package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlaybackState
import com.example.ui.components.SonoraAlbumArt
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraHeartActive
import com.example.ui.theme.SonoraProgressBackground
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Barra mini-reproductor persistente en Sonora.
 * Permite controlar la reproducción rápidamente desde cualquier pantalla
 * y expandir el reproductor completo al tocar la tarjeta.
 */
@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack = playbackState.currentTrack

    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (currentTrack == null) return@AnimatedVisibility

        val borderGradient = Brush.verticalGradient(
            listOf(
                Color(0x38FFFFFF),
                Color(0x0AFFFFFF)
            )
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SonoraSurfaceElevated),
            border = BorderStroke(1.dp, borderGradient),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = SonoraEmerald.copy(alpha = 0.25f)
                )
                .clickable(onClick = onOpenPlayer)
                .testTag("mini_player_bar")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Carátula de la pista actual
                    SonoraAlbumArt(
                        albumArtPath = currentTrack.albumArtPath,
                        modifier = Modifier.size(46.dp),
                        cornerRadius = 8.dp,
                        elevation = 3.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Información del tema
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = SonoraTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentTrack.artist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SonoraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botón de favorito
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (currentTrack.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (currentTrack.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                            tint = if (currentTrack.isFavorite) SonoraHeartActive else SonoraTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Botón de Play / Pause con relieve semi-3D circular
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(SonoraEmeraldBright, SonoraEmerald)
                                )
                            )
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("mini_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Botón Siguiente
                    IconButton(
                        onClick = onPlayNext,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Siguiente canción",
                            tint = SonoraTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Barra de progreso delgada en la base del mini-reproductor
                LinearProgressIndicator(
                    progress = { playbackState.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = SonoraEmerald,
                    trackColor = SonoraProgressBackground
                )
            }
        }
    }
}
