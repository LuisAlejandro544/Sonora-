package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.equalizer.EqualizerBandInfo
import com.example.player.equalizer.EqualizerPreset
import com.example.player.equalizer.EqualizerState
import com.example.player.equalizer.SONORA_10_BANDS
import com.example.sonora.nativeengine.SonoraCppBridge
import com.example.ui.components.Semi3DCard
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import java.util.Locale

/**
 * ==============================================================================
 * PANTALLA MODULAR DEL ECUALIZADOR NATIVO DSP C++ (10 BANDAS)
 * ==============================================================================
 * Permite al usuario manipular el procesamiento de audio en tiempo real:
 * - Curva de respuesta en frecuencia interactiva (B-spline Canvas).
 * - 10 bandas ISO estándar (31 Hz a 16 kHz) con rango de -12 dB a +12 dB.
 * - Selector de ajustes preestablecidos (Presets acústicos).
 * - Preamplificación maestra con compensación de volumen.
 * - Refuerzo de sub-graves analógico (Low-Shelf a 80 Hz).
 * - Limitador analógico Soft-Clipping (tanh) para evitar distorsión digital.
 * - Todos los controles táctiles cumplen ergonomía móvil (mínimo 48dp).
 * ==============================================================================
 */
@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onBandGainChanged: (Int, Float) -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onPreampChanged: (Float) -> Unit,
    onBassBoostChanged: (Float) -> Unit,
    onToggleSoftClip: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isNativeAvailable = remember { SonoraCppBridge.isAvailable() }
    val engineInfo = remember {
        if (isNativeAvailable) SonoraCppBridge.nativeGetCppEngineInfo() else "Modo Simulado"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .testTag("equalizer_screen_root")
    ) {
        // Cabecera con título, estado del motor e interruptor maestro
        EqualizerHeader(
            isEnabled = equalizerState.isEnabled,
            onToggleEnabled = onToggleEnabled,
            onReset = onReset
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tarjeta con información del motor C++ (32/64 bits)
        NativeEngineStatusBadge(engineInfo = engineInfo, isAvailable = isNativeAvailable)

        Spacer(modifier = Modifier.height(16.dp))

        // Carrusel horizontal de ajustes preestablecidos (Presets)
        PresetsSelector(
            currentPreset = equalizerState.currentPreset,
            isEnabled = equalizerState.isEnabled,
            onPresetSelected = onPresetSelected
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Visualización de la curva de respuesta en frecuencia
        FrequencyCurveDisplay(
            bandGains = equalizerState.bandGains,
            isEnabled = equalizerState.isEnabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sección interactiva de las 10 bandas de ecualización
        Text(
            text = "Bandas de Frecuencia (ISO)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SonoraTextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TenBandSlidersSection(
            bandGains = equalizerState.bandGains,
            isEnabled = equalizerState.isEnabled,
            onBandGainChanged = onBandGainChanged
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mejoras acústicas: Preamp, Bass Boost y Soft-Clipping
        Text(
            text = "Dinámica & Saturación Analógica",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SonoraTextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        AcousticDynamicsControls(
            preampDb = equalizerState.preampDb,
            bassBoostDb = equalizerState.bassBoostDb,
            isSoftClipEnabled = equalizerState.isSoftClipEnabled,
            isEnabled = equalizerState.isEnabled,
            onPreampChanged = onPreampChanged,
            onBassBoostChanged = onBassBoostChanged,
            onToggleSoftClip = onToggleSoftClip
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

/**
 * Cabecera principal del ecualizador.
 */
@Composable
private fun EqualizerHeader(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = SonoraEmeraldBright,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ecualizador DSP",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SonoraTextPrimary
                )
            }
            Text(
                text = if (isEnabled) "Procesamiento nativo C++ activo" else "Ecualizador desactivado (Bypass)",
                fontSize = 13.sp,
                color = if (isEnabled) SonoraEmerald else SonoraTextMuted
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("eq_reset_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restablecer a plano",
                    tint = SonoraTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SonoraTextPrimary,
                    checkedTrackColor = SonoraEmerald,
                    uncheckedThumbColor = SonoraTextMuted,
                    uncheckedTrackColor = SonoraSurfaceElevated
                ),
                modifier = Modifier.testTag("eq_master_switch")
            )
        }
    }
}

/**
 * Insignia con el estado de la librería nativa C++ e información de arquitectura (32/64 bits).
 */
@Composable
private fun NativeEngineStatusBadge(engineInfo: String, isAvailable: Boolean) {
    Semi3DCard(
        elevation = 3.dp,
        cornerRadius = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isAvailable) SonoraEmeraldBright else Color(0xFFFF6B6B))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isAvailable) "Motor C++ de Alta Fidelidad (libsonora_dsp.so)" else "C++ No Disponible",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SonoraTextPrimary
                )
                Text(
                    text = engineInfo,
                    fontSize = 11.sp,
                    color = SonoraTextSecondary
                )
            }
        }
    }
}

/**
 * Carrusel horizontal de ajustes preestablecidos (Presets acústicos).
 */
@Composable
private fun PresetsSelector(
    currentPreset: EqualizerPreset,
    isEnabled: Boolean,
    onPresetSelected: (EqualizerPreset) -> Unit
) {
    val scrollState = rememberScrollState()

    Column {
        Text(
            text = "Ajustes Preestablecidos",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = SonoraTextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqualizerPreset.values().forEach { preset ->
                val isSelected = currentPreset == preset
                val shape = RoundedCornerShape(12.dp)

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(shape)
                        .background(
                            if (isSelected) SonoraEmerald.copy(alpha = 0.22f)
                            else SonoraSurfaceElevated
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) SonoraEmeraldBright else Color(0x1FFFFFFF),
                            shape = shape
                        )
                        .clickable(enabled = isEnabled) { onPresetSelected(preset) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SonoraEmeraldBright else if (isEnabled) SonoraTextPrimary else SonoraTextMuted
                    )
                }
            }
        }
    }
}

/**
 * Curva continua gráfica dibujada en Canvas que interpola visualmente las 10 bandas de ganancia.
 */
@Composable
private fun FrequencyCurveDisplay(
    bandGains: List<Float>,
    isEnabled: Boolean
) {
    Semi3DCard(
        elevation = 6.dp,
        cornerRadius = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f

                // Línea central guía (0 dB)
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, centerY),
                    end = Offset(w, centerY),
                    strokeWidth = 1.5f
                )

                // Líneas guías de +6dB y -6dB
                val offset6dB = (h / 2f) * (6f / 12f)
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, centerY - offset6dB),
                    end = Offset(w, centerY - offset6dB),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, centerY + offset6dB),
                    end = Offset(w, centerY + offset6dB),
                    strokeWidth = 1f
                )

                if (bandGains.isEmpty()) return@Canvas

                val stepX = w / (bandGains.size - 1).coerceAtLeast(1)
                val points = bandGains.mapIndexed { index, gain ->
                    val normalizedGain = (gain / 12f).coerceIn(-1f, 1f)
                    val y = centerY - (normalizedGain * (h / 2f * 0.85f))
                    Offset(index * stepX, y)
                }

                // Construir la curva suave con cúbica de Bézier
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlX1 = p0.x + (p1.x - p0.x) / 2f
                        val controlY1 = p0.y
                        val controlX2 = p0.x + (p1.x - p0.x) / 2f
                        val controlY2 = p1.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    }
                }

                val curveColor = if (isEnabled) SonoraEmeraldBright else SonoraTextMuted.copy(alpha = 0.5f)

                drawPath(
                    path = path,
                    color = curveColor,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Dibujar nodos en cada banda
                points.forEach { pt ->
                    drawCircle(
                        color = if (isEnabled) SonoraEmerald else SonoraTextMuted,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

/**
 * Contenedor de deslizadores individuales para las 10 bandas ISO.
 * Diseñado con deslizadores horizontales intuitivos para pantalla táctil móvil.
 */
@Composable
private fun TenBandSlidersSection(
    bandGains: List<Float>,
    isEnabled: Boolean,
    onBandGainChanged: (Int, Float) -> Unit
) {
    Semi3DCard(
        elevation = 6.dp,
        cornerRadius = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SONORA_10_BANDS.forEach { band ->
                val currentGain = bandGains.getOrElse(band.index) { 0f }
                SingleBandControlRow(
                    band = band,
                    gainDb = currentGain,
                    isEnabled = isEnabled,
                    onGainChanged = { newGain -> onBandGainChanged(band.index, newGain) }
                )
            }
        }
    }
}

/**
 * Fila táctil para una banda de frecuencia individual con su deslizador y valor en dB.
 */
@Composable
private fun SingleBandControlRow(
    band: EqualizerBandInfo,
    gainDb: Float,
    isEnabled: Boolean,
    onGainChanged: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Etiqueta de la frecuencia (ej: "1 kHz")
        Text(
            text = band.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isEnabled) SonoraTextPrimary else SonoraTextMuted,
            modifier = Modifier.width(62.dp)
        )

        // Deslizador suave de ganancia (-12 dB a +12 dB)
        Slider(
            value = gainDb,
            onValueChange = onGainChanged,
            valueRange = -12.0f..12.0f,
            enabled = isEnabled,
            colors = SliderDefaults.colors(
                thumbColor = if (isEnabled) SonoraEmeraldBright else SonoraTextMuted,
                activeTrackColor = if (isEnabled) SonoraEmerald else SonoraSurfaceHighlight,
                inactiveTrackColor = SonoraSurfaceHighlight
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("eq_slider_band_${band.index}")
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Valor exacto en dB con formato +0.0 / -0.0
        val formattedGain = String.format(
            Locale.US,
            "%s%.1f dB",
            if (gainDb > 0.05f) "+" else "",
            gainDb
        )
        Text(
            text = formattedGain,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isEnabled) {
                if (Math.abs(gainDb) > 0.1f) SonoraEmeraldBright else SonoraTextSecondary
            } else SonoraTextMuted,
            modifier = Modifier.width(58.dp)
        )
    }
}

/**
 * Controles de dinámica acústica: Preamplificación, Refuerzo de graves (Bass Boost) y Soft-Clipping.
 */
@Composable
private fun AcousticDynamicsControls(
    preampDb: Float,
    bassBoostDb: Float,
    isSoftClipEnabled: Boolean,
    isEnabled: Boolean,
    onPreampChanged: (Float) -> Unit,
    onBassBoostChanged: (Float) -> Unit,
    onToggleSoftClip: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Preamplificación Maestra
        Semi3DCard(elevation = 5.dp, cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preamplificador Maestro",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "%s%.1f dB", if (preampDb > 0.05f) "+" else "", preampDb),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) SonoraEmeraldBright else SonoraTextMuted
                    )
                }
                Slider(
                    value = preampDb,
                    onValueChange = onPreampChanged,
                    valueRange = -12.0f..12.0f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = SonoraEmeraldBright,
                        activeTrackColor = SonoraEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("eq_preamp_slider")
                )
            }
        }

        // Refuerzo de Graves (Bass Boost)
        Semi3DCard(elevation = 5.dp, cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Refuerzo Sub-Graves (Low-Shelf 80 Hz)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "+%.1f dB", bassBoostDb),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled && bassBoostDb > 0f) SonoraEmeraldBright else SonoraTextMuted
                    )
                }
                Slider(
                    value = bassBoostDb,
                    onValueChange = onBassBoostChanged,
                    valueRange = 0.0f..12.0f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = SonoraEmeraldBright,
                        activeTrackColor = SonoraEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("eq_bass_boost_slider")
                )
            }
        }

        // Limitador analógico Soft-Clipping (tanh)
        Semi3DCard(elevation = 5.dp, cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protección Soft-Clipping (tanh)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = "Elimina la distorsión digital áspera al saturar frecuencias graves",
                        fontSize = 12.sp,
                        color = SonoraTextSecondary
                    )
                }
                Switch(
                    checked = isSoftClipEnabled,
                    onCheckedChange = onToggleSoftClip,
                    enabled = isEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SonoraTextPrimary,
                        checkedTrackColor = SonoraEmerald
                    ),
                    modifier = Modifier.testTag("eq_soft_clip_switch")
                )
            }
        }
    }
}
