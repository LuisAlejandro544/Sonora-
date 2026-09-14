package com.example.player.equalizer

/**
 * ==============================================================================
 * MODELO DE DATOS PARA EL ECUALIZADOR NATIVO DE 10 BANDAS DE SONORA
 * ==============================================================================
 * Define las frecuencias centrales estándar ISO, los ajustes preestablecidos
 * (presets acústicos), y el estado reactivo del ecualizador.
 * ==============================================================================
 */

/**
 * Información de una banda individual del ecualizador gráfico.
 */
data class EqualizerBandInfo(
    val index: Int,
    val frequencyHz: Float,
    val label: String
)

/**
 * Frecuencias centrales ISO de las 10 bandas de Sonora.
 */
val SONORA_10_BANDS = listOf(
    EqualizerBandInfo(0, 31.25f, "31 Hz"),
    EqualizerBandInfo(1, 62.5f, "62 Hz"),
    EqualizerBandInfo(2, 125.0f, "125 Hz"),
    EqualizerBandInfo(3, 250.0f, "250 Hz"),
    EqualizerBandInfo(4, 500.0f, "500 Hz"),
    EqualizerBandInfo(5, 1000.0f, "1 kHz"),
    EqualizerBandInfo(6, 2000.0f, "2 kHz"),
    EqualizerBandInfo(7, 4000.0f, "4 kHz"),
    EqualizerBandInfo(8, 8000.0f, "8 kHz"),
    EqualizerBandInfo(9, 16000.0f, "16 kHz")
)

/**
 * Ajustes preestablecidos (Presets de ecualización).
 */
enum class EqualizerPreset(
    val displayName: String,
    val gains: List<Float>
) {
    PLANO("Plano", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Refuerzo Graves", listOf(7.0f, 6.0f, 4.5f, 2.5f, 1.0f, 0f, 0f, 0.5f, 1.0f, 1.5f)),
    ROCK("Rock", listOf(5.0f, 3.5f, 1.5f, -0.5f, -1.5f, 0.5f, 2.5f, 4.0f, 5.0f, 5.5f)),
    POP("Pop", listOf(-1.0f, 1.0f, 2.5f, 3.5f, 3.0f, 1.0f, -0.5f, 1.5f, 3.0f, 3.5f)),
    JAZZ("Jazz", listOf(3.0f, 2.0f, 1.0f, 1.5f, -1.0f, -1.0f, 0.5f, 2.0f, 3.5f, 4.0f)),
    ELECTRONIC("Electrónica", listOf(6.5f, 5.0f, 2.0f, 0f, -1.5f, 2.0f, 1.0f, 3.0f, 4.5f, 5.0f)),
    VOCAL("Acústico / Voz", listOf(-2.0f, -1.0f, 0.5f, 2.5f, 4.0f, 4.0f, 3.0f, 2.0f, 1.0f, 0.5f)),
    METAL("Metal", listOf(6.0f, 4.0f, 1.0f, -1.5f, -2.5f, -0.5f, 2.5f, 5.0f, 6.0f, 6.5f)),
    PERSONALIZADO("Personalizado", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
}

/**
 * Estado completo inmutable del ecualizador.
 */
data class EqualizerState(
    val isEnabled: Boolean = true,
    val bandGains: List<Float> = EqualizerPreset.PLANO.gains,
    val preampDb: Float = 0.0f,
    val bassBoostDb: Float = 0.0f,
    val isSoftClipEnabled: Boolean = true,
    val currentPreset: EqualizerPreset = EqualizerPreset.PLANO
)
