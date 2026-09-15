package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.player.equalizer.EqualizerPreset
import com.example.ui.components.EditMetadataDialog
import com.example.ui.player.FullScreenPlayer
import com.example.ui.player.MiniPlayerBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.SonoraNavTab

/**
 * Pantalla raíz de la aplicación Sonora.
 * Administra la navegación entre pantallas modulares (Inicio, Biblioteca, Listas, Ajustes),
 * la persistencia del mini-reproductor y la apertura del reproductor en pantalla completa.
 */
@Composable
fun MainScreen(
    viewModel: MusicViewModel = viewModel()
) {
    val currentTab by viewModel.currentNavTab.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isFullScreenOpen by viewModel.isFullScreenPlayerOpen.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAddedTracks.collectAsStateWithLifecycle()
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsStateWithLifecycle()
    val totalTrackCount by viewModel.totalTrackCount.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val vocalState by viewModel.vocalState.collectAsStateWithLifecycle()
    val trackToEdit by viewModel.trackToEdit.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar notificaciones de usuario
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Interceptar botón atrás si el reproductor a pantalla completa está abierto
    BackHandler(enabled = isFullScreenOpen) {
        viewModel.closeFullScreenPlayer()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SonoraBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini reproductor si hay pista cargada
                MiniPlayerBar(
                    playbackState = playbackState,
                    onOpenPlayer = { viewModel.openFullScreenPlayer() },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onPlayNext = { viewModel.playNext() },
                    onToggleFavorite = {
                        playbackState.currentTrack?.let { viewModel.toggleFavorite(it) }
                    }
                )

                // Barra de navegación inferior con las 4 secciones principales
                NavigationBar(
                    containerColor = SonoraSurface,
                    contentColor = SonoraTextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        Triple(SonoraNavTab.HOME, Icons.Filled.Home, Icons.Outlined.Home),
                        Triple(SonoraNavTab.LIBRARY, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
                        Triple(SonoraNavTab.PLAYLISTS, Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic),
                        Triple(SonoraNavTab.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
                    )

                    tabs.forEach { (tab, filledIcon, outlinedIcon) ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setNavTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlinedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) SonoraEmeraldBright else SonoraTextMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SonoraSurfaceElevated,
                                selectedIconColor = SonoraEmeraldBright,
                                unselectedIconColor = SonoraTextMuted,
                                selectedTextColor = SonoraEmeraldBright,
                                unselectedTextColor = SonoraTextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Contenido dinámico según pestaña seleccionada
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    SonoraNavTab.HOME -> HomeScreen(
                        allTracks = allTracks,
                        favoriteTracks = favoriteTracks,
                        recentlyAdded = recentlyAdded,
                        mostPlayedTracks = mostPlayedTracks,
                        playbackState = playbackState,
                        totalStorageBytes = totalStorageBytes,
                        isImporting = isImporting,
                        onImportUris = { viewModel.importUris(it) },
                        onSeedDemo = { viewModel.seedDemoTracks() },
                        onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                        onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteTrack = { viewModel.deleteTrack(it) },
                        onNavigateToLibrary = { viewModel.setNavTab(SonoraNavTab.LIBRARY) }
                    )

                    SonoraNavTab.LIBRARY -> LibraryScreen(
                        tracks = allTracks,
                        favoriteTracks = favoriteTracks,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        playbackState = playbackState,
                        playlists = playlists,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onImportUris = { viewModel.importUris(it) },
                        onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                        onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteTrack = { viewModel.deleteTrack(it) },
                        onAddToPlaylist = { playlistId, trackId ->
                            viewModel.addTrackToPlaylist(playlistId, trackId)
                        },
                        onEditTrack = { viewModel.setTrackToEdit(it) }
                    )

                    SonoraNavTab.PLAYLISTS -> PlaylistsScreen(
                        playlists = playlists,
                        favoriteTracks = favoriteTracks,
                        selectedPlaylist = selectedPlaylist,
                        selectedPlaylistTracks = selectedPlaylistTracks,
                        playbackState = playbackState,
                        onSelectPlaylist = { viewModel.selectPlaylist(it) },
                        onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                        onPlayAll = { list, shuffle -> viewModel.playAll(list, shuffle) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onRemoveFromPlaylist = { playlistId, trackId ->
                            viewModel.removeTrackFromPlaylist(playlistId, trackId)
                        },
                        onEditTrack = { viewModel.setTrackToEdit(it) }
                    )

                    SonoraNavTab.SETTINGS -> SettingsScreen(
                        totalTracks = totalTrackCount,
                        totalStorageBytes = totalStorageBytes,
                        isGaplessEnabled = playbackState.isGaplessEnabled,
                        onToggleGapless = { viewModel.toggleGapless() },
                        onSeedDemo = { viewModel.seedDemoTracks() }
                    )
                }
            }
        }
    }

    // Modal reproductor a pantalla completa superpuesto (con ecualizador y laboratorio vocal C++ integrados)
    FullScreenPlayer(
        isOpen = isFullScreenOpen,
        playbackState = playbackState,
        equalizerState = equalizerState,
        vocalState = vocalState,
        onToggleEqualizerEnabled = { viewModel.setEqualizerEnabled(it) },
        onBandGainChanged = { band, gain -> viewModel.setEqualizerBandGain(band, gain) },
        onPresetSelected = { viewModel.applyEqualizerPreset(it) },
        onPreampChanged = { viewModel.setEqualizerPreamp(it) },
        onBassBoostChanged = { viewModel.setEqualizerBassBoost(it) },
        onToggleSoftClip = { viewModel.setEqualizerSoftClip(it) },
        onResetEqualizer = { viewModel.resetEqualizer() },
        onToggleVocalEnabled = { viewModel.setVocalEnabled(it) },
        onVocalSpeedChanged = { viewModel.setVocalSpeed(it) },
        onToggleVocalFormantCorrection = { viewModel.setVocalFormantCorrection(it) },
        onVocalIsolationChanged = { viewModel.setVocalIsolation(it) },
        onVocalGainDbChanged = { viewModel.setVocalGainDb(it) },
        onVocalPresetSelected = { viewModel.applyVocalPreset(it) },
        onResetVocalDefault = { viewModel.resetVocalToDefault() },
        onClose = { viewModel.closeFullScreenPlayer() },
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onPlayNext = { viewModel.playNext() },
        onPlayPrevious = { viewModel.playPrevious() },
        onSeekTo = { viewModel.seekTo(it) },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onCycleRepeatMode = { viewModel.cycleRepeatMode() },
        onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
        onToggleFavorite = {
            playbackState.currentTrack?.let { viewModel.toggleFavorite(it) }
        },
        onEditMetadata = {
            playbackState.currentTrack?.let { viewModel.setTrackToEdit(it) }
        }
    )

    // Diálogo flotante para editar metadatos (título, artista, álbum)
    trackToEdit?.let { track ->
        EditMetadataDialog(
            track = track,
            onDismiss = { viewModel.setTrackToEdit(null) },
            onSave = { newTitle, newArtist, newAlbum ->
                viewModel.updateTrackMetadata(track, newTitle, newArtist, newAlbum)
            }
        )
    }
}
