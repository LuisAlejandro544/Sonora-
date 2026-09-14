package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Pantalla de Listas de Reproducción (Playlists) en Sonora.
 * Permite organizar canciones en colecciones personalizadas y acceder rápidamente a favoritos.
 */
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    favoriteTracks: List<TrackEntity>,
    selectedPlaylist: PlaylistEntity?,
    selectedPlaylistTracks: List<TrackEntity>,
    playbackState: PlaybackState,
    onSelectPlaylist: (PlaylistEntity?) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onPlayAll: (List<TrackEntity>, Boolean) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onRemoveFromPlaylist: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    onEditTrack: ((TrackEntity) -> Unit)? = null
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraSurface)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPlaylist == null) {
            // Vista de catálogo de listas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tus Listas",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = SonoraTextPrimary
                )

                // Botón Nueva Lista
                Box(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(SonoraSurfaceElevated)
                        .clickable { showCreateDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("create_playlist_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Crear lista",
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nueva Lista",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = SonoraTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lista especial: Tus Favoritos
                item {
                    Semi3DCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_favorites_item"),
                        elevation = 6.dp,
                        onClick = {
                            if (favoriteTracks.isNotEmpty()) {
                                onPlayAll(favoriteTracks, false)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF880E4F), Color(0xFFE91E63))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favoritos",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Canciones Favoritas",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SonoraTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${favoriteTracks.size} canciones guardadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                            }

                            if (favoriteTracks.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SonoraEmerald),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir favoritos",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Listas creadas por el usuario
                items(playlists, key = { it.id }) { playlist ->
                    Semi3DCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_item_${playlist.id}"),
                        elevation = 4.dp,
                        onClick = { onSelectPlaylist(playlist) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SonoraEmeraldDark, SonoraSurfaceHighlight)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = SonoraEmeraldBright,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SonoraTextPrimary
                                )
                                if (playlist.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = playlist.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SonoraTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeletePlaylist(playlist.id) },
                                modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar lista",
                                    tint = SonoraTextMuted
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Vista detallada de una lista de reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver a listas",
                        tint = SonoraTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPlaylist.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SonoraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${selectedPlaylistTracks.size} canciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }

                if (selectedPlaylistTracks.isNotEmpty()) {
                    Button(
                        onClick = { onPlayAll(selectedPlaylistTracks, false) },
                        colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reproducir", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedPlaylistTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Esta lista está vacía",
                            style = MaterialTheme.typography.titleMedium,
                            color = SonoraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Añade canciones desde la Biblioteca tocando los 3 puntos en cualquier tema.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(selectedPlaylistTracks, key = { it.id }) { track ->
                        val isCurrent = playbackState.currentTrack?.id == track.id
                        SonoraTrackItem(
                            track = track,
                            isPlaying = playbackState.isPlaying,
                            isCurrentTrack = isCurrent,
                            onClick = { onPlayTrack(track, selectedPlaylistTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onDelete = { onRemoveFromPlaylist(selectedPlaylist.id, track.id) },
                            onEditMetadata = { onEditTrack?.invoke(track) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para crear nueva lista
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "Nueva Lista de Reproducción",
                    color = SonoraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Nombre de la lista", color = SonoraTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SonoraEmerald,
                            unfocusedBorderColor = SonoraSurfaceHighlight,
                            focusedTextColor = SonoraTextPrimary,
                            unfocusedTextColor = SonoraTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Descripción (opcional)", color = SonoraTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SonoraEmerald,
                            unfocusedBorderColor = SonoraSurfaceHighlight,
                            focusedTextColor = SonoraTextPrimary,
                            unfocusedTextColor = SonoraTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim(), newPlaylistDesc.trim())
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald)
                ) {
                    Text("Crear", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = SonoraTextSecondary)
                }
            },
            containerColor = SonoraSurfaceElevated
        )
    }
}
