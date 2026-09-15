package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Semi3DCard
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTealAccent
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Sub-pantalla de Ajustes de Audio y Reproducción.
 * Gestiona la reproducción sin pausas (Gapless), detalles de la pila Media3 ExoPlayer,
 * la notificación nativa en segundo plano y los formatos compatibles.
 */
@Composable
fun SettingsAudioScreen(
    isGaplessEnabled: Boolean,
    onToggleGapless: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Manejo de tecla o gesto de retroceso del teléfono
    BackHandler { onNavigateBack() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraSurface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barra superior con botón de retorno ergonómico (mínimo 48dp)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_back_audio_settings")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a Ajustes",
                        tint = SonoraEmeraldBright,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Audio y Reproducción",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = "Motor de sonido, búfer y reproducción en segundo plano",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Reproducción Sin Pausas (Gapless Playback)
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = SonoraEmeraldBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reproducción Sin Pausas",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = if (isGaplessEnabled) "Activo (Cero silencios)" else "Inactivo (Estándar)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGaplessEnabled) SonoraEmeraldBright else SonoraTextMuted
                                )
                            }
                        }
                        Switch(
                            checked = isGaplessEnabled,
                            onCheckedChange = onToggleGapless,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SonoraEmeraldBright,
                                checkedTrackColor = SonoraEmeraldDark,
                                uncheckedThumbColor = SonoraTextMuted,
                                uncheckedTrackColor = SonoraSurfaceHighlight
                            ),
                            modifier = Modifier.testTag("gapless_playback_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Elimina los micro-silencios y retardos entre pistas contiguas precargando el búfer de audio en memoria. Imprescindible para álbumes en directo, pistas continuas y sesiones musicales sin interrupción.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Notificación Nativa y Segundo Plano (Media3 MediaSessionService)
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = SonoraTealAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notificación Nativa de Reproducción",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sonora implementa un servicio en primer plano mediante MediaSessionService de Media3. Al salir o minimizar la aplicación, la música continúa sonando sin interrupciones y dispones de una notificación multimedia completa con:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Carátula del álbum o arte procedural en alta resolución.\n" +
                               "• Título de la pista y artista en tiempo real.\n" +
                               "• Controles de transporte: Anterior, Pausar/Reanudar y Siguiente.\n" +
                               "• Barra de avance (scrubber) y compatibilidad con pantalla de bloqueo de Android 13+.\n" +
                               "• Detención automática inteligente al pausar para ahorrar batería del teléfono.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = SonoraTextMuted
                    )
                }
            }
        }

        // Sección: Motor Multimedia ExoPlayer
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Motor ExoPlayer & Media3",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sonora utiliza la pila multimedia moderna de Google (Media3 1.5.1 y ExoPlayer) para decodificación de audio de latencia ultrabaja, soporte gapless nativo, gestión inteligente de foco de audio y renderizadores desacoplados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Formatos de audio soportados
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = SonoraEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Formatos Compatibles",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• MP3 (.mp3) - MPEG-1 Audio Layer III\n" +
                               "• FLAC (.flac) - Audio sin pérdidas de alta resolución (24-bit)\n" +
                               "• WAV (.wav) - Modulación por impulsos codificados (PCM)\n" +
                               "• M4A / AAC (.m4a, .aac) - Advanced Audio Coding\n" +
                               "• OGG / OPUS (.ogg, .opus) - Códecs abiertos de alta eficiencia\n" +
                               "• WebM / Matroska Audio",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = SonoraTextSecondary
                    )
                }
            }
        }
    }
}
