package com.example.player.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.example.sonora.nativeengine.SonoraCppBridge
import java.nio.ByteBuffer

/**
 * ==============================================================================
 * PROCESADOR DE AUDIO DE EXOPLAYER PARA EL ECUALIZADOR C++ (10 BANDAS)
 * ==============================================================================
 * Conecta el pipeline de decodificación PCM de ExoPlayer (Media3) directamente
 * con el motor nativo DSP C++ (libsonora_dsp.so).
 * 
 * Opera sobre buffers directos en memoria (DirectByteBuffer) garantizando:
 * - Cero copias innecesarias en la máquina virtual Java.
 * - Procesamiento en tiempo real a 44.1 kHz / 48 kHz estéreo y mono.
 * - Conmutación fluida cuando el ecualizador está activado o en bypass.
 * ==============================================================================
 */
class Sonora10BandAudioProcessor(
    private val equalizerManager: EqualizerManager
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // El ecualizador nativo C++ procesa PCM de 16 bits
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        // Retornamos el mismo formato de salida que de entrada
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        // Asignar el búfer de salida utilizando el mecanismo de BaseAudioProcessor
        val outputBuffer = replaceOutputBuffer(remainingBytes)

        val isEqActive = equalizerManager.isAudioProcessorActive()

        if (!isEqActive || !SonoraCppBridge.isAvailable()) {
            // Modo Bypass: Copia directa del flujo de entrada al flujo de salida
            outputBuffer.put(inputBuffer)
        } else {
            val sampleRate = inputAudioFormat.sampleRate
            val channelCount = inputAudioFormat.channelCount
            val bandGains = equalizerManager.getCachedGains()
            val preamp = equalizerManager.getCachedPreampDb()
            val bassBoost = equalizerManager.getCachedBassBoostDb()
            val softClip = equalizerManager.isCachedSoftClipEnabled()

            if (inputBuffer.isDirect && outputBuffer.isDirect) {
                // Procesamiento nativo directo en C++ a través de punteros de memoria
                SonoraCppBridge.nativeProcessDirectPcm10Band(
                    inputBuffer = inputBuffer,
                    inputOffset = inputBuffer.position(),
                    outputBuffer = outputBuffer,
                    outputOffset = outputBuffer.position(),
                    numBytes = remainingBytes,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    bandGainsDb = bandGains,
                    preampDb = preamp,
                    bassBoostDb = bassBoost,
                    softClipEnabled = softClip
                )
                // Ajustar las posiciones de los búferes
                inputBuffer.position(inputBuffer.limit())
                outputBuffer.position(outputBuffer.position() + remainingBytes)
            } else {
                // Fallback seguro para búferes no directos en memoria de pila
                val shortCount = remainingBytes / 2
                val shortArray = ShortArray(shortCount)
                val shortBuffer = inputBuffer.asShortBuffer()
                shortBuffer.get(shortArray)

                SonoraCppBridge.nativeProcessPcm10Band(
                    pcmSamples = shortArray,
                    numSamples = shortCount,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    bandGainsDb = bandGains,
                    preampDb = preamp,
                    bassBoostDb = bassBoost,
                    softClipEnabled = softClip
                )

                outputBuffer.asShortBuffer().put(shortArray)
                inputBuffer.position(inputBuffer.limit())
                outputBuffer.position(outputBuffer.position() + remainingBytes)
            }
        }

        // Preparar el búfer de salida para el consumidor de audio (DefaultAudioSink / AudioTrack)
        outputBuffer.flip()
    }

    override fun onFlush() {
        super.onFlush()
        SonoraCppBridge.nativeReset10BandEqualizer()
    }

    override fun onReset() {
        super.onReset()
        SonoraCppBridge.nativeReset10BandEqualizer()
    }
}
