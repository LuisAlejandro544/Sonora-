package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.local.TrackEntity
import com.example.player.PlaybackState
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraHeartActive
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTealAccent
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Sección modular "Más escuchadas" (Heavy Rotation) para la pantalla de Inicio de Sonora.
 * 
 * Presenta las canciones con mayor número de reproducciones en una lista jerárquica semi-3D,
 * con insignias metálicas de posición (#1, #2, #3), contador visible de reproducciones
 * y accesos directos táctiles optimizados para el uso ergonómico en dispositivos móviles.
 */
@Composable
fun MostPlayedSection(
    mostPlayedTracks: List<TrackEntity>,
    allTracks: List<TrackEntity>,
    playbackState: PlaybackState,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onPlayAll: (List<TrackEntity>, Boolean) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Si no hay canciones en la biblioteca en absoluto, no mostramos la sección
    if (allTracks.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Cabecera de la sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SonoraEmeraldBright, SonoraTealAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Más escuchadas",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = "Tus favoritas en alta rotación",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SonoraTextSecondary
                    )
                }
            }

            if (mostPlayedTracks.isNotEmpty()) {
                // Botón rápido para reproducir todas las más escuchadas
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SonoraSurfaceHighlight)
                        .clickable { onPlayAll(mostPlayedTracks, false) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("play_all_most_played_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir top",
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reproducir top",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SonoraEmeraldBright
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (mostPlayedTracks.isNotEmpty()) {
            // Lista vertical de pistas más escuchadas con tarjetas semi-3D
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                mostPlayedTracks.take(5).forEachIndexed { index, track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    MostPlayedTrackCard(
                        position = index + 1,
                        track = track,
                        isPlaying = isCurrent && playbackState.isPlaying,
                        isCurrent = isCurrent,
                        onClick = { onPlayTrack(track, mostPlayedTracks) },
                        onToggleFavorite = { onToggleFavorite(track) }
                    )
                }
            }
        } else {
            // Tarjeta informativa cuando aún no se han registrado reproducciones
            Semi3DCard(
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SonoraSurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = SonoraTealAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Construyendo tu historial",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = SonoraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "A medida que escuches tus temas, aquí aparecerá tu podio con los más reproducidos.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SonoraTextSecondary
                        )
                    }

                    Button(
                        onClick = { onPlayAll(allTracks, true) },
                        colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("shuffle_start_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Iniciar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual para una canción en el ranking de más escuchadas.
 * Cuenta con insignia de posición (#1 oro, #2 cian/plata, #3 bronce), contador de reproducciones
 * y soporte táctil de al menos 48dp.
 */
@Composable
private fun MostPlayedTrackCard(
    position: Int,
    track: TrackEntity,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Configuración de insignia de podio según la posición
    val (badgeColors, badgeTextColor) = when (position) {
        1 -> listOf(Color(0xFFFFD700), Color(0xFFFFA000)) to Color.Black
        2 -> listOf(SonoraTealAccent, Color(0xFF00ACC1)) to Color.Black
        3 -> listOf(Color(0xFFFF8A65), Color(0xFFD84315)) to Color.White
        else -> listOf(SonoraSurfaceHighlight, SonoraSurfaceElevated) to SonoraTextSecondary
    }

    Semi3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("most_played_track_${track.id}"),
        elevation = if (isCurrent) 6.dp else 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Insignia numérica de posición
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(badgeColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$position",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    ),
                    color = badgeTextColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Carátula del álbum con indicador de reproducción superpuesto
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                SonoraAlbumArt(
                    albumArtPath = track.albumArtPath,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 8.dp
                )

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioVisualizerBars(
                            isPlaying = true,
                            barColor = SonoraEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Datos de la canción (Título, Artista y Contador de reproducciones)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isCurrent) SonoraEmeraldBright else SonoraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SonoraTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Píldora del contador de reproducciones
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCurrent) SonoraEmerald.copy(alpha = 0.2f)
                                else SonoraSurfaceHighlight
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${track.playCount} reprod.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isCurrent) SonoraEmeraldBright else SonoraTealAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de Favorito
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("favorite_btn_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "Quitar de favoritos" else "Marcar como favorita",
                    tint = if (track.isFavorite) SonoraHeartActive else SonoraTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
