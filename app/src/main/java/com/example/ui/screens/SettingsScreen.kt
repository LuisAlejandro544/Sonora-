package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Semi3DCard
import com.example.ui.screens.settings.SettingsAboutScreen
import com.example.ui.screens.settings.SettingsAudioScreen
import com.example.ui.screens.settings.SettingsNativeScreen
import com.example.ui.screens.settings.SettingsStorageScreen
import com.example.ui.screens.settings.SettingsUiScreen
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTealAccent
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Secciones modulares de configuración técnica de Sonora.
 */
enum class SettingsSection(val title: String) {
    AUDIO("Audio y Reproducción"),
    NATIVE_ENGINE("Aceleración Nativa"),
    UI_ERGONOMICS("Interfaz y Ergonomía"),
    STORAGE("Almacenamiento Local"),
    ABOUT("Privacidad y Acerca de")
}

/**
 * Pantalla principal de Ajustes e Información de Sonora.
 * Presenta una navegación modular estructurada por pantallas independientes
 * para mantener la interfaz despejada, ergonómica y lista para futuras opciones.
 */
@Composable
fun SettingsScreen(
    totalTracks: Int,
    totalStorageBytes: Long?,
    isGaplessEnabled: Boolean = true,
    onToggleGapless: (Boolean) -> Unit = {},
    onSeedDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado de la subpantalla activa seleccionada por el usuario
    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }

    // Interceptar botón atrás físico/gestual del teléfono cuando hay una subpantalla abierta
    BackHandler(enabled = currentSection != null) {
        currentSection = null
    }

    AnimatedContent(
        targetState = currentSection,
        transitionSpec = {
            if (targetState != null) {
                // Al entrar a una subpantalla: deslizar hacia la izquierda y fundir
                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                // Al volver al listado principal: deslizar hacia la derecha y fundir
                (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "settings_subscreen_transition",
        modifier = modifier.fillMaxSize()
    ) { section ->
        when (section) {
            null -> {
                // Vista principal con menú de categorías modular
                SettingsMenuScreen(
                    totalTracks = totalTracks,
                    isGaplessEnabled = isGaplessEnabled,
                    onSelectSection = { currentSection = it }
                )
            }
            SettingsSection.AUDIO -> {
                SettingsAudioScreen(
                    isGaplessEnabled = isGaplessEnabled,
                    onToggleGapless = onToggleGapless,
                    onNavigateBack = { currentSection = null }
                )
            }
            SettingsSection.NATIVE_ENGINE -> {
                SettingsNativeScreen(
                    onNavigateBack = { currentSection = null }
                )
            }
            SettingsSection.UI_ERGONOMICS -> {
                SettingsUiScreen(
                    onNavigateBack = { currentSection = null }
                )
            }
            SettingsSection.STORAGE -> {
                SettingsStorageScreen(
                    totalTracks = totalTracks,
                    totalStorageBytes = totalStorageBytes,
                    onSeedDemo = onSeedDemo,
                    onNavigateBack = { currentSection = null }
                )
            }
            SettingsSection.ABOUT -> {
                SettingsAboutScreen(
                    onNavigateBack = { currentSection = null }
                )
            }
        }
    }
}

/**
 * Menú principal de ajustes con tarjetas semi-3D clickeables e insignias informativas.
 */
@Composable
private fun SettingsMenuScreen(
    totalTracks: Int,
    isGaplessEnabled: Boolean,
    onSelectSection: (SettingsSection) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SonoraSurface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Ajustes e Información",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = SonoraTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configuración técnica, motores y personalización del reproductor",
                style = MaterialTheme.typography.bodySmall,
                color = SonoraTextSecondary
            )
        }

        // 1. Audio y Reproducción
        item {
            SettingsCategoryCard(
                icon = Icons.Default.GraphicEq,
                iconTint = SonoraEmeraldBright,
                title = "Audio y Reproducción",
                description = "Reproducción sin pausas (Gapless), pila Media3, notificación en segundo plano y formatos de audio.",
                badgeText = if (isGaplessEnabled) "Gapless Activo" else "Estándar",
                testTag = "settings_cat_audio",
                onClick = { onSelectSection(SettingsSection.AUDIO) }
            )
        }

        // 2. Aceleración Nativa (C++ & Rust)
        item {
            SettingsCategoryCard(
                icon = Icons.Default.Memory,
                iconTint = SonoraEmerald,
                title = "Aceleración Nativa",
                description = "Motor C++ (DSP & Vocal), Rust (FFT & Hash), detección de 32/64 bits y test de rendimiento.",
                badgeText = "C++17 y Rust Activos",
                testTag = "settings_cat_native",
                onClick = { onSelectSection(SettingsSection.NATIVE_ENGINE) }
            )
        }

        // 3. Interfaz y Ergonomía
        item {
            SettingsCategoryCard(
                icon = Icons.Default.FormatSize,
                iconTint = SonoraTealAccent,
                title = "Interfaz y Ergonomía",
                description = "Aislamiento tipográfico calibrado (1.0x), diseño semi-3D y controles táctiles de 48dp.",
                badgeText = "Calibrado 1.0x",
                testTag = "settings_cat_ui",
                onClick = { onSelectSection(SettingsSection.UI_ERGONOMICS) }
            )
        }

        // 4. Almacenamiento Local
        item {
            SettingsCategoryCard(
                icon = Icons.Default.Folder,
                iconTint = SonoraEmeraldBright,
                title = "Almacenamiento Local",
                description = "Espacio en disco, pistas de prueba demo, 4 carpetas modulares y compresión WebP Lossless.",
                badgeText = "$totalTracks temas",
                testTag = "settings_cat_storage",
                onClick = { onSelectSection(SettingsSection.STORAGE) }
            )
        }

        // 5. Privacidad y Acerca de
        item {
            SettingsCategoryCard(
                icon = Icons.Default.Lock,
                iconTint = SonoraTextMuted,
                title = "Privacidad y Acerca de",
                description = "Funcionamiento 100% offline, distribución libre en Uptodown y versión de Sonora v1.0.",
                badgeText = "100% Offline",
                testTag = "settings_cat_about",
                onClick = { onSelectSection(SettingsSection.ABOUT) }
            )
        }
    }
}

/**
 * Tarjeta interactiva con relieve semi-3D y objetivo táctil ergonómico (> 48dp)
 * para acceder a una categoría de ajustes.
 */
@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Semi3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SonoraSurfaceHighlight, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SonoraEmeraldBright
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Abrir sección",
                tint = SonoraTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
