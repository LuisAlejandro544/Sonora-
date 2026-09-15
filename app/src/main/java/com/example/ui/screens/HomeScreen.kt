package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.components.MostPlayedSection
import com.example.ui.components.Semi3DCard
import com.example.ui.components.SonoraAlbumArt
import com.example.ui.components.SonoraTrackItem
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraHeartActive
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTealAccent
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import java.util.Calendar

/**
 * Pantalla de Inicio (Home) de Sonora.
 * Ofrece un resumen interactivo al estilo streaming con tarjetas semi-3D,
 * acceso rápido para importar canciones locales y accesos directos a reproducción.
 */
@Composable
fun HomeScreen(
    allTracks: List<TrackEntity>,
    favoriteTracks: List<TrackEntity>,
    recentlyAdded: List<TrackEntity>,
    mostPlayedTracks: List<TrackEntity> = emptyList(),
    playbackState: PlaybackState,
    totalStorageBytes: Long?,
    isImporting: Boolean,
    onImportUris: (List<android.net.Uri>) -> Unit,
    onSeedDemo: () -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onPlayAll: (List<TrackEntity>, Boolean) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Selector de documentos del sistema (MIME audio/*)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportUris(uris)
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Buenos días"
            in 12..19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraSurface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cabecera de saludo con diseño moderno
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = "Tu música local, sin conexión y con máxima calidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }

                // Indicador de importación activa si está procesando
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = SonoraEmeraldBright,
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }

        // Fila de tarjetas semi-3D de acceso rápido
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tarjeta 1: Importar archivos de música
                Semi3DCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .testTag("home_import_card"),
                    elevation = 6.dp,
                    onClick = {
                        documentPickerLauncher.launch(arrayOf("audio/*"))
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        SonoraEmeraldDark.copy(alpha = 0.5f),
                                        SonoraSurfaceElevated
                                    )
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SonoraEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Importar",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Importar",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SonoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Tarjeta 2: Canciones Favoritas
                Semi3DCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .testTag("home_favorites_card"),
                    elevation = 6.dp,
                    onClick = {
                        if (favoriteTracks.isNotEmpty()) {
                            onPlayAll(favoriteTracks, false)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4A1525).copy(alpha = 0.5f),
                                        SonoraSurfaceElevated
                                    )
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE91E63)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favoritos",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Favoritos",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SonoraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${favoriteTracks.size} temas",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = SonoraTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Tarjeta 3: Modo Aleatorio
                Semi3DCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .testTag("home_shuffle_card"),
                    elevation = 6.dp,
                    onClick = {
                        if (allTracks.isNotEmpty()) {
                            onPlayAll(allTracks, true)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0D47A1).copy(alpha = 0.4f),
                                        SonoraSurfaceElevated
                                    )
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SonoraTealAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Aleatorio",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Aleatorio",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SonoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Banner informativo o de bienvenida si aún no hay canciones importadas
        if (allTracks.isEmpty()) {
            item {
                Semi3DCard(
                    elevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tu biblioteca está lista",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sonora no escanea tus archivos automáticamente. Selecciona tus audios descargados para importarlos con portadas y metadatos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { documentPickerLauncher.launch(arrayOf("audio/*")) },
                                colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Importar archivos", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSeedDemo,
                                colors = ButtonDefaults.buttonColors(containerColor = SonoraSurfaceHighlight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Probar demos", color = SonoraTextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Sección: Canciones recientemente añadidas (Carrusel Horizontal)
        if (recentlyAdded.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Añadidas recientemente",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = "Ver todas",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = SonoraEmeraldBright,
                            modifier = Modifier.clickable(onClick = onNavigateToLibrary)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(recentlyAdded, key = { it.id }) { item ->
                            val isCurrent = playbackState.currentTrack?.id == item.id
                            Semi3DCard(
                                modifier = Modifier
                                    .width(140.dp)
                                    .testTag("recent_card_${item.id}"),
                                elevation = 4.dp,
                                onClick = { onPlayTrack(item, recentlyAdded) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        SonoraAlbumArt(
                                            albumArtPath = item.albumArtPath,
                                            modifier = Modifier.fillMaxSize(),
                                            cornerRadius = 10.dp
                                        )
                                        // Botón de reproducción superpuesto
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .size(32.dp)
                                                .shadow(4.dp, CircleShape)
                                                .clip(CircleShape)
                                                .background(SonoraEmerald),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Reproducir",
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (isCurrent) SonoraEmeraldBright else SonoraTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = SonoraTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sección: Métricas y Resumen de biblioteca local
        if (allTracks.isNotEmpty()) {
            item {
                Semi3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${allTracks.size}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SonoraEmeraldBright
                                )
                            )
                            Text(
                                text = "Canciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(SonoraSurfaceHighlight)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalDurationMs = allTracks.sumOf { it.durationMs }
                            Text(
                                text = formatDuration(totalDurationMs),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SonoraTextPrimary
                                )
                            )
                            Text(
                                text = "Duración total",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(SonoraSurfaceHighlight)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatFileSize(totalStorageBytes ?: 0L),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SonoraTealAccent
                                )
                            )
                            Text(
                                text = "Espacio usado",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Sección: Canciones más escuchadas (Heavy Rotation / Top reproducidas)
        if (allTracks.isNotEmpty()) {
            item {
                MostPlayedSection(
                    mostPlayedTracks = mostPlayedTracks,
                    allTracks = allTracks,
                    playbackState = playbackState,
                    onPlayTrack = onPlayTrack,
                    onPlayAll = onPlayAll,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}
