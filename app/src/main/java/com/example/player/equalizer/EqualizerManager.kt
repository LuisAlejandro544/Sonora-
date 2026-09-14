package com.example.player.equalizer

import android.content.Context
import android.content.SharedPreferences
import com.example.sonora.nativeengine.SonoraCppBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ==============================================================================
 * GESTOR DEL ECUALIZADOR NATIVO DE 10 BANDAS (EqualizerManager)
 * ==============================================================================
 * Administra los parámetros del DSP de audio en tiempo real:
 * - Emite estados reactivos (StateFlow) para la interfaz de usuario en Jetpack Compose.
 * - Almacena de forma persistente la configuración en SharedPreferences.
 * - Proporciona lecturas seguras y de baja latencia para el hilo de procesamiento de ExoPlayer.
 * ==============================================================================
 */
class EqualizerManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _equalizerState = MutableStateFlow(loadInitialState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    // Variables cacheadas en memoria para acceso ultra-rápido desde el hilo de audio PCM
    @Volatile
    private var cachedGains: FloatArray = _equalizerState.value.bandGains.toFloatArray()

    @Volatile
    private var cachedPreampDb: Float = _equalizerState.value.preampDb

    @Volatile
    private var cachedBassBoostDb: Float = _equalizerState.value.bassBoostDb

    @Volatile
    private var cachedSoftClipEnabled: Boolean = _equalizerState.value.isSoftClipEnabled

    @Volatile
    private var cachedIsEnabled: Boolean = _equalizerState.value.isEnabled

    private fun loadInitialState(): EqualizerState {
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val preamp = prefs.getFloat(KEY_PREAMP, 0.0f)
        val bassBoost = prefs.getFloat(KEY_BASS_BOOST, 0.0f)
        val softClip = prefs.getBoolean(KEY_SOFT_CLIP, true)
        val presetName = prefs.getString(KEY_PRESET, EqualizerPreset.PLANO.name) ?: EqualizerPreset.PLANO.name

        val gains = mutableListOf<Float>()
        for (i in 0 until 10) {
            val gain = prefs.getFloat("${KEY_BAND_PREFIX}_$i", 0.0f)
            gains.add(gain)
        }

        val preset = try {
            EqualizerPreset.valueOf(presetName)
        } catch (_: Exception) {
            EqualizerPreset.PLANO
        }

        return EqualizerState(
            isEnabled = enabled,
            bandGains = gains,
            preampDb = preamp,
            bassBoostDb = bassBoost,
            isSoftClipEnabled = softClip,
            currentPreset = preset
        )
    }

    /**
     * Activa o desactiva globalmente el ecualizador nativo.
     */
    fun setEnabled(enabled: Boolean) {
        cachedIsEnabled = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _equalizerState.update { it.copy(isEnabled = enabled) }
        if (!enabled) {
            SonoraCppBridge.nativeReset10BandEqualizer()
        }
    }

    /**
     * Ajusta la ganancia en decibelios (-12 dB a +12 dB) de una banda específica (0 a 9).
     */
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until 10) return
        val clamped = gainDb.coerceIn(-12.0f, 12.0f)

        _equalizerState.update { current ->
            val updated = current.bandGains.toMutableList()
            updated[bandIndex] = clamped

            cachedGains = updated.toFloatArray()
            prefs.edit().putFloat("${KEY_BAND_PREFIX}_$bandIndex", clamped).apply()

            // Si se modifica manualmente, pasa a estado PERSONALIZADO si no coincide
            current.copy(
                bandGains = updated,
                currentPreset = EqualizerPreset.PERSONALIZADO
            )
        }
    }

    /**
     * Aplica un ajuste preestablecido (Preset) completo.
     */
    fun applyPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.PERSONALIZADO) return

        val newGains = preset.gains
        cachedGains = newGains.toFloatArray()

        val editor = prefs.edit()
        editor.putString(KEY_PRESET, preset.name)
        newGains.forEachIndexed { i, gain ->
            editor.putFloat("${KEY_BAND_PREFIX}_$i", gain)
        }
        editor.apply()

        _equalizerState.update { current ->
            current.copy(
                bandGains = newGains,
                currentPreset = preset
            )
        }
    }

    /**
     * Modifica la ganancia de preamplificación (-12 dB a +12 dB).
     */
    fun setPreamp(preampDb: Float) {
        val clamped = preampDb.coerceIn(-12.0f, 12.0f)
        cachedPreampDb = clamped
        prefs.edit().putFloat(KEY_PREAMP, clamped).apply()
        _equalizerState.update { it.copy(preampDb = clamped) }
    }

    /**
     * Modifica el refuerzo de sub-graves Bass Boost (0 dB a +12 dB).
     */
    fun setBassBoost(bassBoostDb: Float) {
        val clamped = bassBoostDb.coerceIn(0.0f, 12.0f)
        cachedBassBoostDb = clamped
        prefs.edit().putFloat(KEY_BASS_BOOST, clamped).apply()
        _equalizerState.update { it.copy(bassBoostDb = clamped) }
    }

    /**
     * Activa o desactiva la protección contra clipping digital por Soft-Clipping analógico.
     */
    fun setSoftClipEnabled(enabled: Boolean) {
        cachedSoftClipEnabled = enabled
        prefs.edit().putBoolean(KEY_SOFT_CLIP, enabled).apply()
        _equalizerState.update { it.copy(isSoftClipEnabled = enabled) }
    }

    /**
     * Restablece el ecualizador a su curva plana neutra (0 dB).
     */
    fun resetToFlat() {
        applyPreset(EqualizerPreset.PLANO)
        setPreamp(0.0f)
        setBassBoost(0.0f)
        setSoftClipEnabled(true)
        SonoraCppBridge.nativeReset10BandEqualizer()
    }

    // Métodos de acceso rápido para el AudioProcessor de ExoPlayer
    fun isAudioProcessorActive(): Boolean = cachedIsEnabled

    fun getCachedGains(): FloatArray = cachedGains

    fun getCachedPreampDb(): Float = cachedPreampDb

    fun getCachedBassBoostDb(): Float = cachedBassBoostDb

    fun isCachedSoftClipEnabled(): Boolean = cachedSoftClipEnabled

    companion object {
        private const val PREFS_NAME = "sonora_equalizer_preferences"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PREAMP = "eq_preamp"
        private const val KEY_BASS_BOOST = "eq_bass_boost"
        private const val KEY_SOFT_CLIP = "eq_soft_clip"
        private const val KEY_PRESET = "eq_current_preset"
        private const val KEY_BAND_PREFIX = "eq_band_gain"

        @Volatile
        private var INSTANCE: EqualizerManager? = null

        fun getInstance(context: Context): EqualizerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EqualizerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
