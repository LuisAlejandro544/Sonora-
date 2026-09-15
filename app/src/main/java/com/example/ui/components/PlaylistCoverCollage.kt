package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.TrackEntity
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import java.io.File

/**
 * ==============================================================================
 * CARÁTULA DINÁMICA CON COLLAGE PARA LISTAS DE REPRODUCCIÓN (PLAYLISTS)
 * ==============================================================================
 * Reglas de renderizado solicitadas:
 * 1. Si la lista tiene una carátula personalizada (convertida a WebP Lossless),
 *    se muestra esa carátula personalizada directamente.
 * 2. Si no tiene carátula personalizada:
 *    - 0 canciones: Muestra el icono y degradado insignia de Sonora.
 *    - 1 canción: Muestra la carátula de esa única canción ocupando el 100%.
 *    - 2 canciones: Collage horizontal de 2 fotos (50% / 50%).
 *    - 3 o más canciones: Collage de 3 fotos (máximo 3 fotos, 1 grande a la
 *      izquierda y 2 cuadrantes a la derecha). No se agregan más de 3 fotos.
 * ==============================================================================
 */
@Composable
fun PlaylistCoverCollage(
    customCoverPath: String?,
    tracks: List<TrackEntity>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 4.dp
) {
    val context = LocalContext.current
    val hasCustomCover = remember(customCoverPath) {
        if (!customCoverPath.isNullOrBlank()) {
            File(customCoverPath).exists()
        } else {
            false
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(SonoraSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        if (hasCustomCover) {
            // Carátula personalizada cargada y comprimida en WebP Lossless
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(customCoverPath!!))
                    .crossfade(true)
                    .error(R.drawable.img_default_cover)
                    .build(),
                contentDescription = "Carátula personalizada de la lista",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Collage dinámico según la cantidad de canciones (máximo 3 fotos)
            val collageTracks = remember(tracks) { tracks.take(3) }
            val count = collageTracks.size

            when (count) {
                0 -> {
                    // Sin canciones: Fondo con gradiente esmeralda e icono de lista
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(SonoraEmeraldDark, SonoraSurfaceHighlight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Lista vacía",
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                1 -> {
                    // 1 canción: 1 sola foto ocupando todo el recuadro
                    CollageTrackImage(track = collageTracks[0], modifier = Modifier.fillMaxSize())
                }

                2 -> {
                    // 2 canciones: Collage de 2 fotos divididas 50% / 50%
                    Row(modifier = Modifier.fillMaxSize()) {
                        CollageTrackImage(
                            track = collageTracks[0],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(SonoraBackground)
                        )
                        CollageTrackImage(
                            track = collageTracks[1],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                else -> {
                    // 3 o más canciones: Collage de 3 fotos (máximo 3, aunque haya 4 o más)
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Foto 1: Mitad izquierda completa
                        CollageTrackImage(
                            track = collageTracks[0],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(SonoraBackground)
                        )
                        // Fotos 2 y 3: Mitad derecha dividida verticalmente
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            CollageTrackImage(
                                track = collageTracks[1],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(SonoraBackground)
                            )
                            CollageTrackImage(
                                track = collageTracks[2],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renderiza la carátula de una pista individual dentro de una celda del collage.
 * Si no tiene carátula propia, recurre a la carátula predeterminada con recorte adecuado.
 */
@Composable
private fun CollageTrackImage(
    track: TrackEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageModel = remember(track.albumArtPath) {
        if (!track.albumArtPath.isNullOrBlank()) {
            val file = File(track.albumArtPath)
            if (file.exists()) file else R.drawable.img_default_cover
        } else {
            R.drawable.img_default_cover
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageModel)
            .crossfade(true)
            .error(R.drawable.img_default_cover)
            .build(),
        contentDescription = track.title,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
