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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sonora.nativeengine.NativeEngineManager
import com.example.ui.components.Semi3DCard
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTealAccent
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Sub-pantalla de Aceleración Nativa (C++ y Rust).
 * Muestra el estado en caliente de las librerías dinámicas .so, arquitectura de CPU (32 vs 64 bits)
 * y ejecuta la suite de benchmark interactivo en hardware real.
 */
@Composable
fun SettingsNativeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

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
                        .testTag("btn_back_native_settings")
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
                        text = "Aceleración Nativa",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = "Motores de alto rendimiento en C++17 y Rust (O3)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Estado del Motor C++ (DSP, Ecualizador 10 bandas y Laboratorio Vocal)
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
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = SonoraEmeraldBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Motor C++ (DSP & Vocal)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SonoraTextPrimary
                            )
                        }
                        Text(
                            text = if (nativeStatus.cppLoaded) "Activo (C++17)" else "Inactivo",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (nativeStatus.cppLoaded) SonoraEmeraldBright else Color(0xFFFF5252)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nativeStatus.cppInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Filtros Biquad IIR paramétricos basados en fórmulas Robert Bristow-Johnson.\n" +
                               "• Limitador analógico suave Soft-Clipping (tanh) para evitar distorsión.\n" +
                               "• Procesamiento PCM vocal con preservación de formantes acústicos (Anti-Ardilla).",
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 18.sp),
                        color = SonoraTextMuted
                    )
                }
            }
        }

        // Estado del Motor Rust (FFT, dBFS y Hash acústico)
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
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = SonoraTealAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Motor Rust (Acústica)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SonoraTextPrimary
                            )
                        }
                        Text(
                            text = if (nativeStatus.rustLoaded) "Activo (O3)" else "Inactivo",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (nativeStatus.rustLoaded) SonoraEmeraldBright else Color(0xFFFF5252)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nativeStatus.rustInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Transformada Rápida de Fourier (FFT) a 60 FPS con ventana Hamming.\n" +
                               "• Medición de energía acústica RMS y cálculo de decibelios dBFS.\n" +
                               "• Extracción nativa de metadatos ID3v2/Vorbis y hash de audio FNV-1a de 64 bits.",
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 18.sp),
                        color = SonoraTextMuted
                    )
                }
            }
        }

        // Arquitectura de CPU del dispositivo
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
                            tint = SonoraEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Arquitectura del Procesador",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ABI detectada: ${nativeStatus.deviceAbi} (${if (nativeStatus.is64Bit) "Modo 64 bits de alto rendimiento" else "Modo 32 bits compatible"})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SonoraEmeraldBright
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sonora compila para las 4 ABIs de Android (arm64-v8a, armeabi-v7a, x86_64, x86) asegurando compatibilidad óptima en cualquier dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Suite de Benchmark Interactivo
        item {
            Semi3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Test de Rendimiento DSP",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SonoraTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ejecuta un ciclo de procesamiento intensivo de buffers PCM simulando 10 bandas Biquad y transformada FFT para medir la velocidad de ejecución en nanosegundos.",
                        style = MaterialTheme.typography.bodySmall,
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
                            .height(48.dp)
                            .testTag("run_native_benchmark_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Iniciar Benchmark C++ vs Rust",
                            color = SonoraTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (benchmarkLog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = benchmarkLog,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            ),
                            color = SonoraEmeraldBright,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F171A), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
