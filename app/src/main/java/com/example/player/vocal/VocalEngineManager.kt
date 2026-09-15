package com.example.player.vocal

import android.content.Context
import android.content.SharedPreferences
import com.example.sonora.nativeengine.SonoraCppBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ==============================================================================
 * GESTOR DEL MOTOR VOCAL NATIVO C++ (VocalEngineManager)
 * ==============================================================================
 * Administra los parámetros en tiempo real del motor vocal C++:
 * - Emite estados reactivos (StateFlow) para la interfaz de Compose.
 * - Sincroniza configuraciones con el binario C++ libsonora_dsp.so.
 * - Cachea valores en memoria volátil para el hilo de procesamiento de audio.
 * ==============================================================================
 */
class VocalEngineManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _vocalState = MutableStateFlow(loadInitialState())
    val vocalState: StateFlow<VocalEngineState> = _vocalState.asStateFlow()

    // Variables cacheadas en memoria volátil para acceso inmediato desde el hilo de audio
    @Volatile
    private var cachedIsEnabled: Boolean = _vocalState.value.isEnabled

    @Volatile
    private var cachedVocalSpeed: Float = _vocalState.value.vocalSpeed

    @Volatile
    private var cachedFormantCorrection: Boolean = _vocalState.value.isFormantCorrectionEnabled

    @Volatile
    private var cachedVocalIsolation: Float = _vocalState.value.vocalIsolation

    @Volatile
    private var cachedVocalGainDb: Float = _vocalState.value.vocalGainDb

    init {
        syncToNativeEngine()
    }

    private fun loadInitialState(): VocalEngineState {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val speed = prefs.getFloat(KEY_SPEED, 1.0f)
        val formant = prefs.getBoolean(KEY_FORMANT, true)
        val isolation = prefs.getFloat(KEY_ISOLATION, 0.85f)
        val gainDb = prefs.getFloat(KEY_GAIN_DB, 0.0f)
        val presetName = prefs.getString(KEY_PRESET, VocalPreset.ORIGINAL.name) ?: VocalPreset.ORIGINAL.name

        val preset = try {
            VocalPreset.valueOf(presetName)
        } catch (_: Exception) {
            VocalPreset.ORIGINAL
        }

        return VocalEngineState(
            isEnabled = enabled,
            vocalSpeed = speed,
            isFormantCorrectionEnabled = formant,
            vocalIsolation = isolation,
            vocalGainDb = gainDb,
            currentPreset = preset
        )
    }

    fun isAudioProcessorActive(): Boolean = cachedIsEnabled

    /**
     * Activa o desactiva el procesamiento vocal C++.
     */
    fun setEnabled(enabled: Boolean) {
        cachedIsEnabled = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _vocalState.update { it.copy(isEnabled = enabled) }
        syncToNativeEngine()
        if (!enabled) {
            SonoraCppBridge.nativeResetVocalEngine()
        }
    }

    /**
     * Modifica la velocidad de la voz (0.50x a 2.00x).
     */
    fun setVocalSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.50f, 2.00f)
        cachedVocalSpeed = clamped
        prefs.edit().putFloat(KEY_SPEED, clamped).apply()
        _vocalState.update {
            it.copy(
                vocalSpeed = clamped,
                currentPreset = determineMatchingPreset(clamped, it.isFormantCorrectionEnabled, it.vocalIsolation, it.vocalGainDb)
            )
        }
        syncToNativeEngine()
    }

    /**
     * Activa o desactiva la corrección de formantes humanos (Anti-Ardilla).
     */
    fun setFormantCorrectionEnabled(enabled: Boolean) {
        cachedFormantCorrection = enabled
        prefs.edit().putBoolean(KEY_FORMANT, enabled).apply()
        _vocalState.update {
            it.copy(
                isFormantCorrectionEnabled = enabled,
                currentPreset = determineMatchingPreset(it.vocalSpeed, enabled, it.vocalIsolation, it.vocalGainDb)
            )
        }
        syncToNativeEngine()
    }

    /**
     * Ajusta el nivel de aislamiento del canal central vocal (0.0 a 1.0).
     */
    fun setVocalIsolation(isolation: Float) {
        val clamped = isolation.coerceIn(0.0f, 1.0f)
        cachedVocalIsolation = clamped
        prefs.edit().putFloat(KEY_ISOLATION, clamped).apply()
        _vocalState.update {
            it.copy(
                vocalIsolation = clamped,
                currentPreset = determineMatchingPreset(it.vocalSpeed, it.isFormantCorrectionEnabled, clamped, it.vocalGainDb)
            )
        }
        syncToNativeEngine()
    }

    /**
     * Ajusta la ganancia de presencia de la voz (-6.0 dB a +6.0 dB).
     */
    fun setVocalGainDb(gainDb: Float) {
        val clamped = gainDb.coerceIn(-6.0f, 6.0f)
        cachedVocalGainDb = clamped
        prefs.edit().putFloat(KEY_GAIN_DB, clamped).apply()
        _vocalState.update {
            it.copy(
                vocalGainDb = clamped,
                currentPreset = determineMatchingPreset(it.vocalSpeed, it.isFormantCorrectionEnabled, it.vocalIsolation, clamped)
            )
        }
        syncToNativeEngine()
    }

    /**
     * Aplica un preset estilizado de voz.
     */
    fun applyPreset(preset: VocalPreset) {
        if (preset == VocalPreset.PERSONALIZADO) return

        cachedVocalSpeed = preset.speed
        cachedFormantCorrection = preset.formantCorrection
        cachedVocalIsolation = preset.isolation
        cachedVocalGainDb = preset.gainDb

        prefs.edit()
            .putFloat(KEY_SPEED, preset.speed)
            .putBoolean(KEY_FORMANT, preset.formantCorrection)
            .putFloat(KEY_ISOLATION, preset.isolation)
            .putFloat(KEY_GAIN_DB, preset.gainDb)
            .putString(KEY_PRESET, preset.name)
            .apply()

        _vocalState.update {
            it.copy(
                vocalSpeed = preset.speed,
                isFormantCorrectionEnabled = preset.formantCorrection,
                vocalIsolation = preset.isolation,
                vocalGainDb = preset.gainDb,
                currentPreset = preset
            )
        }
        syncToNativeEngine()
    }

    /**
     * Restablece la configuración a valores originales de fábrica.
     */
    fun resetToDefault() {
        applyPreset(VocalPreset.ORIGINAL)
        SonoraCppBridge.nativeResetVocalEngine()
    }

    private fun syncToNativeEngine() {
        if (SonoraCppBridge.isAvailable()) {
            SonoraCppBridge.nativeSetVocalEngineConfig(
                enabled = cachedIsEnabled,
                speed = cachedVocalSpeed,
                formantCorrection = cachedFormantCorrection,
                isolation = cachedVocalIsolation,
                gainDb = cachedVocalGainDb
            )
        }
    }

    private fun determineMatchingPreset(
        speed: Float,
        formant: Boolean,
        isolation: Float,
        gainDb: Float
    ): VocalPreset {
        return VocalPreset.values().firstOrNull { preset ->
            preset != VocalPreset.PERSONALIZADO &&
                Math.abs(preset.speed - speed) < 0.03f &&
                preset.formantCorrection == formant &&
                Math.abs(preset.isolation - isolation) < 0.05f &&
                Math.abs(preset.gainDb - gainDb) < 0.2f
        } ?: VocalPreset.PERSONALIZADO
    }

    companion object {
        private const val PREFS_NAME = "sonora_vocal_engine_prefs"
        private const val KEY_ENABLED = "vocal_enabled"
        private const val KEY_SPEED = "vocal_speed"
        private const val KEY_FORMANT = "vocal_formant"
        private const val KEY_ISOLATION = "vocal_isolation"
        private const val KEY_GAIN_DB = "vocal_gain_db"
        private const val KEY_PRESET = "vocal_preset"

        @Volatile
        private var INSTANCE: VocalEngineManager? = null

        fun getInstance(context: Context): VocalEngineManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VocalEngineManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
