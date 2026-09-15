package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.vocal.VocalEngineState
import com.example.player.vocal.VocalPreset
import com.example.sonora.nativeengine.SonoraCppBridge
import com.example.ui.components.Semi3DCard
import com.example.ui.theme.*
import java.util.Locale

/**
 * ==============================================================================
 * PANTALLA DE LABORATORIO VOCAL NATIVO C++ (VocalLabScreen)
 * ==============================================================================
 * Permite modificar la velocidad y cadencia de la voz del cantante:
 * - Sin acelerar ni desacelerar el tempo general de la canción.
 * - Con algoritmo nativo de preservación de formantes acústicos (Anti-Ardilla).
 * - Control de aislamiento vocal del canal central y ganancia de presencia.
 * - Diseñado para una ergonomía táctil óptima en teléfonos móviles.
 * ==============================================================================
 */
@Composable
fun VocalLabScreen(
    vocalState: VocalEngineState,
    onToggleEnabled: (Boolean) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onToggleFormantCorrection: (Boolean) -> Unit,
    onIsolationChanged: (Float) -> Unit,
    onGainDbChanged: (Float) -> Unit,
    onPresetSelected: (VocalPreset) -> Unit,
    onResetDefault: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cppInfo: String = remember { SonoraCppBridge.getEngineInfo() }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Barra Superior con botón de cierre amplio y título ergonómico
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("vocal_lab_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver al reproductor",
                        tint = SonoraTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Laboratorio Vocal C++",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = "Velocidad de voz sin alterar la música",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraEmeraldBright
                    )
                }
            }

            // Botón de restablecimiento
            TextButton(
                onClick = onResetDefault,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("vocal_reset_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Restablecer",
                    tint = SonoraTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Restablecer",
                    color = SonoraTextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Chip informativo de hardware C++ y arquitectura de CPU
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SonoraSurfaceElevated.copy(alpha = 0.7f))
                .border(1.dp, SonoraBorderMuted, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Motor Nativo C++",
                    tint = SonoraCyanBright,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = cppInfo,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = SonoraTextSecondary,
                    maxLines = 1
                )
            }
        }

        // Contenido scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta Principal: Activación Maestro del Motor Vocal
            Semi3DCard(
                elevation = 6.dp,
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (vocalState.isEnabled) SonoraEmeraldBright else SonoraTextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Procesador Vocal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SonoraTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (vocalState.isEnabled) {
                                "Activo en tiempo real sobre ExoPlayer (Cero latencia)"
                            } else {
                                "Desactivado (Flujo de audio original en bypass)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (vocalState.isEnabled) SonoraEmeraldBright else SonoraTextMuted
                        )
                    }

                    Switch(
                        checked = vocalState.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = SonoraEmeraldBright,
                            uncheckedThumbColor = SonoraTextMuted,
                            uncheckedTrackColor = SonoraSurfaceElevated
                        ),
                        modifier = Modifier.testTag("vocal_engine_switch")
                    )
                }
            }

            AnimatedVisibility(
                visible = vocalState.isEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // 1. Selector de Presets de Voz Rápidos
                    Column {
                        Text(
                            text = "PREAJUSTES VOCALES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = SonoraTextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(VocalPreset.values().filter { it != VocalPreset.PERSONALIZADO }) { preset ->
                                val isSelected = vocalState.currentPreset == preset
                                val bgBrush = if (isSelected) {
                                    Brush.verticalGradient(listOf(SonoraEmeraldBright, SonoraEmerald))
                                } else {
                                    Brush.verticalGradient(listOf(SonoraSurfaceElevated, SonoraCardBackground))
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(bgBrush)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) SonoraEmeraldBright else SonoraBorderMuted,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { onPresetSelected(preset) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .defaultMinSize(minHeight = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) Color.Black else SonoraTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // 2. Control Principal de Velocidad de la Voz (Anti-Ardilla)
                    Semi3DCard(
                        elevation = 6.dp,
                        cornerRadius = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = SonoraCyanBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Velocidad de la Voz",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SonoraTextPrimary
                                    )
                                }

                                // Indicador visual del valor actual
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SonoraCyanBright.copy(alpha = 0.15f))
                                        .border(1.dp, SonoraCyanBright, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.2fx", vocalState.vocalSpeed),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SonoraCyanBright
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Acelera o desacelera el canto vocal en tiempo real. La música y los instrumentos de fondo permanecen a velocidad original.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Slider Ergonómico de Velocidad con botones +/-
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onSpeedChanged((vocalState.vocalSpeed - 0.05f).coerceAtLeast(0.50f)) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("vocal_speed_minus_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Disminuir velocidad de voz",
                                        tint = SonoraTextPrimary
                                    )
                                }

                                Slider(
                                    value = vocalState.vocalSpeed,
                                    onValueChange = onSpeedChanged,
                                    valueRange = 0.50f..2.00f,
                                    steps = 29, // Incrementos de 0.05
                                    colors = SliderDefaults.colors(
                                        thumbColor = SonoraCyanBright,
                                        activeTrackColor = SonoraCyanBright,
                                        inactiveTrackColor = SonoraProgressBackground
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("vocal_speed_slider")
                                )

                                IconButton(
                                    onClick = { onSpeedChanged((vocalState.vocalSpeed + 0.05f).coerceAtMost(2.00f)) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("vocal_speed_plus_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Aumentar velocidad de voz",
                                        tint = SonoraTextPrimary
                                    )
                                }
                            }

                            // Marcas de referencia rápida (0.75x, 1.0x, 1.25x, 1.5x)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(0.50f to "0.5x", 1.00f to "1.0x", 1.50f to "1.5x", 2.00f to "2.0x").forEach { (speed, label) ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (Math.abs(vocalState.vocalSpeed - speed) < 0.04f) SonoraCyanBright else SonoraTextMuted,
                                        modifier = Modifier.clickable { onSpeedChanged(speed) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Tarjeta de Corrección de Formantes (Anti-Ardilla C++)
                    Semi3DCard(
                        elevation = 6.dp,
                        cornerRadius = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = if (vocalState.isFormantCorrectionEnabled) SonoraEmeraldBright else SonoraTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Filtro Anti-Ardilla (Formantes)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SonoraTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (vocalState.isFormantCorrectionEnabled) {
                                        "Corrige las frecuencias de laringe y cavidad torácica humana para evitar tonos agudos o chillones al acelerar."
                                    } else {
                                        "Desactivado: La voz subirá de tono libremente produciendo el efecto cómico de ardilla."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (vocalState.isFormantCorrectionEnabled) SonoraEmeraldBright else SonoraTextMuted
                                )
                            }

                            Switch(
                                checked = vocalState.isFormantCorrectionEnabled,
                                onCheckedChange = onToggleFormantCorrection,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = SonoraEmeraldBright,
                                    uncheckedThumbColor = SonoraTextMuted,
                                    uncheckedTrackColor = SonoraSurfaceElevated
                                ),
                                modifier = Modifier.testTag("formant_correction_switch")
                            )
                        }
                    }

                    // 4. Parámetros Acústicos Avanzados (Aislamiento y Ganancia)
                    Semi3DCard(
                        elevation = 6.dp,
                        cornerRadius = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "CALIBRACIÓN ACÚSTICA AVANZADA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = SonoraTextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Slider: Aislamiento del Canal Central
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Aislamiento Canal Central",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = "${(vocalState.vocalIsolation * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SonoraEmeraldBright
                                )
                            }
                            Slider(
                                value = vocalState.vocalIsolation,
                                onValueChange = onIsolationChanged,
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SonoraEmeraldBright,
                                    activeTrackColor = SonoraEmerald,
                                    inactiveTrackColor = SonoraProgressBackground
                                ),
                                modifier = Modifier.testTag("vocal_isolation_slider")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Slider: Ganancia de Presencia Vocal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Presencia de la Voz (dB)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = String.format(Locale.US, "%+.1f dB", vocalState.vocalGainDb),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SonoraCyanBright
                                )
                            }
                            Slider(
                                value = vocalState.vocalGainDb,
                                onValueChange = onGainDbChanged,
                                valueRange = -6.0f..6.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SonoraCyanBright,
                                    activeTrackColor = SonoraCyanBright,
                                    inactiveTrackColor = SonoraProgressBackground
                                ),
                                modifier = Modifier.testTag("vocal_gain_slider")
                            )
                        }
                    }
                }
            }
        }
    }
}
