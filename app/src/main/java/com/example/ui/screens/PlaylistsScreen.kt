package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.TrackEntity
import com.example.player.PlaybackState
import com.example.ui.components.AddSongsToPlaylistDialog
import com.example.ui.components.PlaylistCoverCollage
import com.example.ui.components.Semi3DCard
import com.example.ui.components.SonoraTrackItem
import com.example.ui.components.formatDuration
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.viewmodel.MusicViewModel

/**
 * ==============================================================================
 * PANTALLA DE LISTAS DE REPRODUCCIÓN (PLAYLISTS)
 * ==============================================================================
 * Ofrece gestión integral de listas personalizadas estilo Spotify:
 * - Vista de catálogo con tarjeta de favoritos y listas personalizadas.
 * - Portadas dinámicas con collage inteligente (1 foto si hay 1 tema, 2 si hay 2,
 *   3 fotos si hay 3 o más; si hay más no se agregan más fotos).
 * - Carátula personalizada comprimida a WebP Lossless a máxima calidad.
 * - Vista detallada de lista con botón destacado "+ Añadir canciones" y reproductor.
 * ==============================================================================
 */
@Composable
fun PlaylistsScreen(
    playlistsWithTracks: List<PlaylistWithTracks>,
    favoriteTracks: List<TrackEntity>,
    allTracks: List<TrackEntity>,
    selectedPlaylist: PlaylistEntity?,
    selectedPlaylistTracks: List<TrackEntity>,
    playbackState: PlaybackState,
    onSelectPlaylist: (PlaylistEntity?) -> Unit,
    onCreatePlaylist: (String, String, Uri?) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onPlayAll: (List<TrackEntity>, Boolean) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onRemoveFromPlaylist: (Long, Long) -> Unit,
    onSetCustomCover: (Long, Uri) -> Unit,
    modifier: Modifier = Modifier,
    onEditTrack: ((TrackEntity) -> Unit)? = null,
    onSanitizeTrack: ((TrackEntity) -> Unit)? = null,
    onSyncArtistPlaylists: (() -> Unit)? = null
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    // Selector de fotos para cambiar carátula de lista existente en WebP Lossless
    val changeCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && selectedPlaylist != null && selectedPlaylist.id != MusicViewModel.FAVORITES_PLAYLIST_ID) {
            onSetCustomCover(selectedPlaylist.id, uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraSurface)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPlaylist == null) {
            // ==========================================
            // VISTA 1: CATÁLOGO DE LISTAS
            // ==========================================
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSyncArtistPlaylists != null) {
                        Box(
                            modifier = Modifier
                                .shadow(3.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(SonoraSurfaceElevated)
                                .clickable { onSyncArtistPlaylists() }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("sync_artist_playlists_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Sincronizar artistas",
                                    tint = SonoraEmeraldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto Artistas",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = SonoraEmeraldBright
                                )
                            }
                        }
                    }

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
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta insignia: Canciones Favoritas (acceso directo a todas las canciones con corazón)
                item {
                    Semi3DCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_favorites_item"),
                        elevation = 6.dp,
                        onClick = {
                            onSelectPlaylist(MusicViewModel.FAVORITES_PLAYLIST)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Portada con collage de temas favoritos o corazón estilizado
                            if (favoriteTracks.isNotEmpty()) {
                                PlaylistCoverCollage(
                                    customCoverPath = null,
                                    tracks = favoriteTracks,
                                    modifier = Modifier.size(58.dp),
                                    cornerRadius = 12.dp,
                                    elevation = 2.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
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
                                IconButton(
                                    onClick = { onPlayAll(favoriteTracks, false) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SonoraEmerald)
                                        .testTag("play_favorites_quick_btn")
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

                // Listas creadas por el usuario con collage dinámico (máximo 3 fotos) o carátula WebP
                items(playlistsWithTracks, key = { it.playlist.id }) { item ->
                    val playlist = item.playlist
                    val tracks = item.tracks

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
                            // Carátula con collage inteligente (1, 2 o máximo 3 fotos) o personalizada WebP
                            PlaylistCoverCollage(
                                customCoverPath = playlist.customCoverPath,
                                tracks = tracks,
                                modifier = Modifier.size(58.dp),
                                cornerRadius = 12.dp,
                                elevation = 2.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SonoraTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (playlist.description.startsWith("Colección automática")) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SonoraEmerald.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Artista",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = SonoraEmeraldBright
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                val descText = if (playlist.description.isNotBlank()) {
                                    "${playlist.description} • ${tracks.size} canciones"
                                } else {
                                    "${tracks.size} canciones"
                                }
                                Text(
                                    text = descText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { playlistToDelete = playlist },
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
            // ==========================================
            // VISTA 2: PANTALLA DETALLADA DE PLAYLIST
            // ==========================================
            val isFavoritesView = selectedPlaylist.id == MusicViewModel.FAVORITES_PLAYLIST_ID

            // Barra superior de navegación
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSelectPlaylist(null) },
                    modifier = Modifier.testTag("back_to_playlists_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver a listas",
                        tint = SonoraTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isFavoritesView) "Canciones Favoritas" else selectedPlaylist.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = SonoraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cabecera estilizada de la lista
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SonoraSurfaceElevated)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Carátula grande con opción para cambiar carátula a WebP Lossless
                Box(
                    modifier = Modifier.size(105.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    PlaylistCoverCollage(
                        customCoverPath = selectedPlaylist.customCoverPath,
                        tracks = selectedPlaylistTracks,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 14.dp,
                        elevation = 6.dp
                    )

                    if (!isFavoritesView) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SonoraSurface.copy(alpha = 0.85f))
                                .clickable {
                                    changeCoverLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("change_playlist_cover_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Cambiar carátula (WebP Lossless)",
                                tint = SonoraEmeraldBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFavoritesView) "Canciones Favoritas" else selectedPlaylist.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SonoraTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedPlaylist.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedPlaylist.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val totalDuration = selectedPlaylistTracks.sumOf { it.durationMs }
                    Text(
                        text = "${selectedPlaylistTracks.size} canciones • ${formatDuration(totalDuration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SonoraEmeraldBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fila de botones de acción rápida estilo Spotify (+ Añadir canciones, Reproducir, Aleatorio)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedPlaylistTracks.isNotEmpty()) {
                    Button(
                        onClick = { onPlayAll(selectedPlaylistTracks, false) },
                        colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("play_all_playlist_btn")
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

                    IconButton(
                        onClick = { onPlayAll(selectedPlaylistTracks, true) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SonoraSurfaceHighlight)
                            .testTag("shuffle_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isFavoritesView) {
                    OutlinedButton(
                        onClick = { showAddSongsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraEmeraldBright),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_songs_to_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Añadir canciones", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de canciones asociadas a la playlist
            if (selectedPlaylistTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = if (isFavoritesView) "Aún no tienes canciones favoritas" else "Esta lista está vacía",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isFavoritesView) {
                                "Marca el corazón en cualquier canción para que aparezca aquí automáticamente."
                            } else {
                                "Toca \"+ Añadir canciones\" para armar tu lista con los temas que quieras."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextMuted,
                            textAlign = TextAlign.Center
                        )
                        if (!isFavoritesView) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddSongsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir canciones ahora", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
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
                            onDelete = {
                                if (isFavoritesView) {
                                    onToggleFavorite(track)
                                } else {
                                    onRemoveFromPlaylist(selectedPlaylist.id, track.id)
                                }
                            },
                            onEditMetadata = { onEditTrack?.invoke(track) },
                            onSanitizeWithRust = { onSanitizeTrack?.invoke(track) }
                        )
                    }
                }
            }
        }
    }

    // Modal para agregar canciones a la lista estilo Spotify
    if (showAddSongsDialog && selectedPlaylist != null) {
        AddSongsToPlaylistDialog(
            isOpen = showAddSongsDialog,
            playlistName = selectedPlaylist.name,
            allTracks = allTracks,
            currentPlaylistTrackIds = selectedPlaylistTracks.map { it.id }.toSet(),
            onAddTrack = { track ->
                onAddTrackToPlaylist(selectedPlaylist.id, track.id)
            },
            onDismiss = { showAddSongsDialog = false }
        )
    }

    // Diálogo para crear una nueva playlist con carátula personalizada opcional
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, coverUri ->
                onCreatePlaylist(name, desc, coverUri)
                showCreateDialog = false
            }
        )
    }

    // Diálogo de confirmación para eliminar playlist
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = {
                Text(
                    text = "Eliminar lista",
                    color = SonoraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Deseas eliminar la lista \"${playlist.name}\"? Las canciones seguirán existiendo en tu biblioteca.",
                    color = SonoraTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(playlist.id)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancelar", color = SonoraTextSecondary)
                }
            },
            containerColor = SonoraSurfaceElevated
        )
    }
}

/**
 * Diálogo modal para crear nueva lista de reproducción, permitiendo adjuntar
 * una carátula personalizada que será procesada a WebP Lossless.
 */
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, coverUri: Uri?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedCoverUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Lista de Reproducción",
                color = SonoraTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Selector de carátula personalizada con previsualización
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SonoraSurface)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SonoraSurfaceHighlight)
                            .border(1.dp, SonoraEmeraldDark, RoundedCornerShape(10.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedCoverUri != null) {
                            AsyncImage(
                                model = selectedCoverUri,
                                contentDescription = "Carátula seleccionada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Elegir foto",
                                tint = SonoraEmeraldBright,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedCoverUri != null) "Carátula lista (WebP Lossless)" else "Carátula personalizada",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = "Se convertirá a WebP sin pérdida",
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextMuted
                        )
                    }

                    if (selectedCoverUri != null) {
                        IconButton(onClick = { selectedCoverUri = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar foto",
                                tint = SonoraTextMuted
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
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
                    value = desc,
                    onValueChange = { desc = it },
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
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), desc.trim(), selectedCoverUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SonoraEmerald)
            ) {
                Text("Crear", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SonoraTextSecondary)
            }
        },
        containerColor = SonoraSurfaceElevated
    )
}
