package com.example.player.vocal

/**
 * ==============================================================================
 * MODELO DE DATOS: MOTOR VOCAL C++ (VELOCIDAD Y ANTI-ARDILLA)
 * ==============================================================================
 * Representa el estado del procesador acústico vocal nativo.
 * ==============================================================================
 */

/**
 * Preajustes estilizados para el control de velocidad y presencia vocal.
 */
enum class VocalPreset(
    val displayName: String,
    val speed: Float,
    val formantCorrection: Boolean,
    val isolation: Float,
    val gainDb: Float,
    val description: String
) {
    ORIGINAL(
        displayName = "Original (1.0x)",
        speed = 1.0f,
        formantCorrection = true,
        isolation = 0.85f,
        gainDb = 0.0f,
        description = "Cadencia natural sin modificaciones"
    ),
    RAPIDA_NATURAL(
        displayName = "Voz Ágil (1.25x)",
        speed = 1.25f,
        formantCorrection = true,
        isolation = 0.85f,
        gainDb = 1.0f,
        description = "Vocalización rápida con formantes anclados"
    ),
    RAPIDA_TURBO(
        displayName = "Voz Rápida (1.50x)",
        speed = 1.50f,
        formantCorrection = true,
        isolation = 0.90f,
        gainDb = 1.5f,
        description = "Ritmo vocal acelerado sin distorsión de tono"
    ),
    LENTA_PROFUNDA(
        displayName = "Voz Pausada (0.80x)",
        speed = 0.80f,
        formantCorrection = true,
        isolation = 0.85f,
        gainDb = 0.5f,
        description = "Cadencia lenta con resonancia torácica cálida"
    ),
    LENTA_ULTRA(
        displayName = "Voz Lenta (0.65x)",
        speed = 0.65f,
        formantCorrection = true,
        isolation = 0.90f,
        gainDb = 1.0f,
        description = "Articulación extendida y presencia analógica"
    ),
    EXPERIMENTAL_RAW(
        displayName = "Sin Anti-Ardilla (Bypass)",
        speed = 1.35f,
        formantCorrection = false,
        isolation = 0.85f,
        gainDb = 0.0f,
        description = "Desplaza formantes naturales (muestra el efecto original)"
    ),
    PERSONALIZADO(
        displayName = "Personalizado",
        speed = 1.0f,
        formantCorrection = true,
        isolation = 0.85f,
        gainDb = 0.0f,
        description = "Calibración manual por el usuario"
    )
}

/**
 * Estado inmutable del motor vocal nativo.
 */
data class VocalEngineState(
    val isEnabled: Boolean = false,
    val vocalSpeed: Float = 1.0f,
    val isFormantCorrectionEnabled: Boolean = true,
    val vocalIsolation: Float = 0.85f,
    val vocalGainDb: Float = 0.0f,
    val currentPreset: VocalPreset = VocalPreset.ORIGINAL
)
