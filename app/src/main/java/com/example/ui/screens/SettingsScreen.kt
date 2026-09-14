package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Semi3DCard
import com.example.ui.components.formatFileSize
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTealAccent
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sonora.nativeengine.NativeEngineManager

/**
 * Pantalla de Ajustes e Información técnica de Sonora.
 * Presenta información sobre el motor ExoPlayer Media3, formatos admitidos,
 * privacidad de archivos y administración de almacenamiento local.
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
    var nativeStatus by remember { mutableStateOf(NativeEngineManager.getStatus()) }
    var benchmarkLog by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraSurface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                text = "Configuración técnica y privacidad del reproductor",
                style = MaterialTheme.typography.bodySmall,
                color = SonoraTextSecondary
            )
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
                        text = "Elimina los micro-silencios y retardos entre pistas contiguas precargando el búfer de audio. Imprescindible para álbumes en directo, pistas continuas y sesiones musicales sin interrupción.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Escala Tipográfica Propia
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                tint = SonoraTealAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tamaño de Letra Propio",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = "Aislamiento tipográfico calibrado (1.0x)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SonoraTealAccent
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sonora utiliza una escala de texto propia calibrada para que los controles, deslizadores del ecualizador y botones táctiles ergonómicos de 48dp no se desborden ni colapsen con la fuente del sistema del teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Motor Multimedia
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
                        text = "Sonora utiliza la pila multimedia moderna de Google (Media3 1.5.1 y ExoPlayer) para decodificación de audio de baja latencia, soporte gapless nativo y gestión de foco de audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Motores Nativos C++ y Rust
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Aceleración Nativa (C++ & Rust)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Estado C++ DSP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = SonoraEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Motor C++ (DSP & Biquad):", style = MaterialTheme.typography.bodySmall, color = SonoraTextSecondary)
                        }
                        Text(
                            text = if (nativeStatus.cppLoaded) "Activo (C++17)" else "Inactivo",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (nativeStatus.cppLoaded) SonoraEmeraldBright else Color(0xFFFF5252)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nativeStatus.cppInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Estado Rust Engine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = SonoraTealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Motor Rust (FFT & Hash):", style = MaterialTheme.typography.bodySmall, color = SonoraTextSecondary)
                        }
                        Text(
                            text = if (nativeStatus.rustLoaded) "Activo (O3)" else "Inactivo",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (nativeStatus.rustLoaded) SonoraEmeraldBright else Color(0xFFFF5252)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nativeStatus.rustInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Arquitectura del dispositivo: ${nativeStatus.deviceAbi} (${if (nativeStatus.is64Bit) "64 bits" else "32 bits"})",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            benchmarkLog = NativeEngineManager.runPerformanceBenchmark()
                            nativeStatus = NativeEngineManager.getStatus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SonoraSurfaceHighlight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_native_benchmark_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test de Rendimiento DSP (C++ vs Rust)", color = SonoraTextPrimary)
                    }

                    if (benchmarkLog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = benchmarkLog,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                lineHeight = 16.sp
                            ),
                            color = SonoraEmeraldBright,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F171A), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                    }
                }
            }
        }

        // Sección: Almacenamiento local
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = SonoraTealAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Almacenamiento Local",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pistas registradas:", style = MaterialTheme.typography.bodyMedium, color = SonoraTextSecondary)
                        Text("$totalTracks temas", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = SonoraTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Espacio ocupado:", style = MaterialTheme.typography.bodyMedium, color = SonoraTextSecondary)
                        Text(formatFileSize(totalStorageBytes ?: 0L), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = SonoraEmeraldBright)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onSeedDemo,
                        colors = ButtonDefaults.buttonColors(containerColor = SonoraSurfaceHighlight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seed_demo_tracks_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar pistas de prueba", color = SonoraTextPrimary)
                    }
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
                               "• FLAC (.flac) - Audio sin pérdidas de alta resolución\n" +
                               "• WAV (.wav) - Modulación por impulsos codificados (PCM)\n" +
                               "• M4A / AAC (.m4a, .aac) - Advanced Audio Coding\n" +
                               "• OGG / OPUS (.ogg, .opus) - Códecs de código abierto\n" +
                               "• WebM / Matroska Audio",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Privacidad y Política
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacidad Estricta",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sonora no escanea ni rastrea tu almacenamiento. Las pistas sólo se importan cuando tú las seleccionas explícitamente a través del selector de archivos de Android. No requiere conexión a internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Sección: Acerca de
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SonoraTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sonora v1.0",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reproductor local con arquitectura Jetpack Compose, Room y Media3 ExoPlayer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextMuted
                    )
                }
            }
        }
    }
}
