package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.TrackEntity
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraHeartActive
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import java.io.File
import java.util.Locale

/**
 * Componentes comunes de interfaz y diseño visual para Sonora.
 * Incluye carátulas de canciones con fallback, filas de canciones con micro-interacciones,
 * tarjetas táctiles semi-3D y formateadores de tiempo y almacenamiento.
 */

/**
 * Formatea milisegundos a formato MM:SS o HH:MM:SS.
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

/**
 * Formatea bytes en KB, MB o GB legibles.
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        else -> String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

/**
 * Componente para mostrar carátulas de canciones.
 * Si la pista tiene una carátula local incrustada, la carga mediante Coil;
 * de lo contrario, muestra la imagen semi-3D predeterminada de Sonora.
 */
@Composable
fun SonoraAlbumArt(
    albumArtPath: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    elevation: Dp = 4.dp
) {
    val context = LocalContext.current
    val imageModel = remember(albumArtPath) {
        if (!albumArtPath.isNullOrBlank()) {
            val file = File(albumArtPath)
            if (file.exists()) file else R.drawable.img_default_cover
        } else {
            R.drawable.img_default_cover
        }
    }

    Box(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(SonoraSurfaceElevated)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .error(R.drawable.img_default_cover)
                .build(),
            contentDescription = "Carátula del álbum",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
    }
}

/**
 * Fila interactiva de pista de audio estilo streaming.
 * Muestra el estado activo de reproducción, botón de favorito y menú contextual.
 */
@Composable
fun SonoraTrackItem(
    track: TrackEntity,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToPlaylist: (() -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentTrack) SonoraSurfaceHighlight.copy(alpha = 0.6f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Carátula pequeña con efecto semi-3D
        Box(modifier = Modifier.size(52.dp)) {
            SonoraAlbumArt(
                albumArtPath = track.albumArtPath,
                modifier = Modifier.size(52.dp),
                cornerRadius = 8.dp,
                elevation = if (isCurrentTrack) 6.dp else 2.dp
            )
            // Indicador si está reproduciéndose
            if (isCurrentTrack && isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    AudioVisualizerBars(
                        isPlaying = true,
                        barColor = SonoraEmeraldBright,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Datos: Título y Artista
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isCurrentTrack) SonoraEmeraldBright else SonoraTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = SonoraTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " • ${formatDuration(track.durationMs)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SonoraTextMuted
                )
            }
        }

        // Botón de Favorito (Corazón verde esmeralda o contorno)
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(48.dp)
                .testTag("track_favorite_btn_${track.id}")
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (track.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                tint = if (track.isFavorite) SonoraEmeraldBright else SonoraTextMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        // Menú de opciones (3 puntos)
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("track_options_btn_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones de pista",
                    tint = SonoraTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(SonoraSurfaceElevated)
            ) {
                if (onEditMetadata != null) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = SonoraEmeraldBright,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Editar metadatos", color = SonoraTextPrimary)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onEditMetadata()
                        }
                    )
                }
                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Añadir a lista", color = SonoraTextPrimary) },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Eliminar de Sonora", color = Color(0xFFFF6B6B)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

/**
 * Visualizador de ecualizador de audio estilizado.
 * Genera pequeñas barras animadas que reaccionan con movimiento fluido cuando la música está activa.
 */
@Composable
fun AudioVisualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = SonoraEmeraldBright,
    barCount: Int = 4
) {
    val transition = rememberInfiniteTransition(label = "equalizer_animation")

    val anim1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 1.0f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val anim2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 0.85f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val anim3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.95f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val anim4 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.7f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val heights = listOf(anim1, anim2, anim3, anim4)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val scale = heights[i % heights.size]
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((20 * scale).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Tarjeta táctil con relieve semi-3D, sutil borde luminoso y sombra envolvente.
 */
@Composable
fun Semi3DCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 6.dp,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0x2EFFFFFF),
            Color(0x08FFFFFF)
        )
    )

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = SonoraSurfaceElevated),
        border = BorderStroke(1.dp, borderBrush),
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black.copy(alpha = 0.8f)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}
