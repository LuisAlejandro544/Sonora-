package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.player.PlaybackState
import com.example.ui.components.Semi3DCard
import com.example.ui.components.SonoraTrackItem
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

enum class TrackSortOption(val displayName: String) {
    RECENT("Añadidas recientemente"),
    TITLE("Título (A-Z)"),
    ARTIST("Artista"),
    DURATION("Mayor duración"),
    MOST_PLAYED("Más escuchadas")
}

/**
 * Pantalla de Biblioteca (Library) de Sonora.
 * Presenta todas las pistas de audio locales importadas, buscador en vivo,
 * filtros rápidos, ordenación y acciones para reproducir e importar canciones.
 */
@Composable
fun LibraryScreen(
    tracks: List<TrackEntity>,
    favoriteTracks: List<TrackEntity>,
    searchQuery: String,
    searchResults: List<TrackEntity>,
    playbackState: PlaybackState,
    playlists: List<PlaylistEntity>,
    onSearchChange: (String) -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onPlayAll: (List<TrackEntity>, Boolean) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    onEditTrack: ((TrackEntity) -> Unit)? = null,
    onSanitizeTrack: ((TrackEntity) -> Unit)? = null,
    onSanitizeAllTracks: (() -> Unit)? = null
) {
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportUris(uris)
        }
    }

    var selectedFilter by remember { mutableStateOf("Todas") }
    var sortOption by remember { mutableStateOf(TrackSortOption.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }

    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }

    // Determinar la lista base según búsqueda o filtro seleccionado
    val baseList = when {
        searchQuery.isNotBlank() -> searchResults
        selectedFilter == "Favoritas" -> favoriteTracks
        else -> tracks
    }

    // Aplicar ordenación
    val displayedTracks = remember(baseList, sortOption) {
        when (sortOption) {
            TrackSortOption.RECENT -> baseList.sortedByDescending { it.dateAdded }
            TrackSortOption.TITLE -> baseList.sortedBy { it.title.lowercase() }
            TrackSortOption.ARTIST -> baseList.sortedBy { it.artist.lowercase() }
            TrackSortOption.DURATION -> baseList.sortedByDescending { it.durationMs }
            TrackSortOption.MOST_PLAYED -> baseList.sortedByDescending { it.playCount }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraSurface)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Título de la pantalla y botón flotante de Importar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tu Biblioteca",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = SonoraTextPrimary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón táctil semi-3D para limpiar metadatos corruptos con Rust
                if (onSanitizeAllTracks != null) {
                    Box(
                        modifier = Modifier
                            .shadow(3.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(SonoraSurfaceElevated)
                            .clickable { onSanitizeAllTracks() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("library_rust_clean_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Limpiar metadatos con Rust",
                                tint = SonoraEmeraldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limpiar Rust",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = SonoraEmeraldBright
                            )
                        }
                    }
                }

                // Botón táctil semi-3D para importar canciones
                Box(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(SonoraEmerald, SonoraEmeraldBright)
                            )
                        )
                        .clickable { documentPickerLauncher.launch(arrayOf("audio/*")) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("library_import_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Importar canciones",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Importar",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Barra de búsqueda en vivo
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar canción, artista o álbum...", color = SonoraTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = SonoraEmeraldBright
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Borrar búsqueda",
                            tint = SonoraTextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SonoraSurfaceElevated,
                unfocusedContainerColor = SonoraSurfaceElevated,
                focusedBorderColor = SonoraEmerald,
                unfocusedBorderColor = SonoraSurfaceHighlight,
                focusedTextColor = SonoraTextPrimary,
                unfocusedTextColor = SonoraTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Fila de Filtros (Chips) y Botón de Ordenación
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Todas", "Favoritas")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else SonoraTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonoraEmerald,
                            containerColor = SonoraSurfaceElevated
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SonoraSurfaceHighlight,
                            selectedBorderColor = SonoraEmerald,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Menú de Ordenación
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("sort_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Ordenar lista",
                        tint = SonoraEmeraldBright
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(SonoraSurfaceElevated)
                ) {
                    TrackSortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    color = if (sortOption == option) SonoraEmeraldBright else SonoraTextPrimary,
                                    fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortOption = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Acciones rápidas: Reproducir todo y Aleatorio (si hay canciones)
        if (displayedTracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onPlayAll(displayedTracks, false) },
                    colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_play_all_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reproducir",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { onPlayAll(displayedTracks, true) },
                    colors = ButtonDefaults.buttonColors(containerColor = SonoraSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = SonoraEmeraldBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aleatorio",
                        color = SonoraTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Lista de canciones
        if (displayedTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No se encontraron resultados" else "No hay canciones aquí",
                        style = MaterialTheme.typography.titleMedium,
                        color = SonoraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Prueba con otro término" else "Usa el botón 'Importar' para añadir tus canciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displayedTracks, key = { it.id }) { track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    SonoraTrackItem(
                        track = track,
                        isPlaying = playbackState.isPlaying,
                        isCurrentTrack = isCurrent,
                        onClick = { onPlayTrack(track, displayedTracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onDelete = { onDeleteTrack(track) },
                        onAddToPlaylist = { trackToAddToPlaylist = track },
                        onEditMetadata = { onEditTrack?.invoke(track) },
                        onSanitizeWithRust = { onSanitizeTrack?.invoke(track) }
                    )
                }
            }
        }
    }

    // Diálogo para agregar pista a lista de reproducción existente
    if (trackToAddToPlaylist != null) {
        val targetTrack = trackToAddToPlaylist!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = {
                Text(
                    "Añadir a lista",
                    color = SonoraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (playlists.isEmpty()) {
                    Text(
                        "No tienes listas creadas aún. Ve a la pestaña 'Listas' para crear una.",
                        color = SonoraTextSecondary
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onAddToPlaylist(playlist.id, targetTrack.id)
                                        trackToAddToPlaylist = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = SonoraTextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { trackToAddToPlaylist = null }) {
                    Text("Cancelar", color = SonoraEmeraldBright)
                }
            },
            containerColor = SonoraSurfaceElevated
        )
    }
}
