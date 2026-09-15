package com.example.player.vocal

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.example.sonora.nativeengine.SonoraCppBridge
import java.nio.ByteBuffer

/**
 * ==============================================================================
 * PROCESADOR DE AUDIO PARA EL MOTOR VOCAL C++ (VELOCIDAD Y ANTI-ARDILLA)
 * ==============================================================================
 * Conecta el flujo PCM de ExoPlayer (Media3) con el motor nativo de C++:
 * - Aísla la componente central vocal de la música.
 * - Modifica la velocidad aparente de la voz del cantante sin acelerar la música.
 * - Corrige los formantes acústicos para evitar el timbre de ardilla.
 * - Opera con búferes directos en memoria (DirectByteBuffer) para máxima eficiencia.
 * ==============================================================================
 */
class SonoraVocalAudioProcessor(
    private val vocalEngineManager: VocalEngineManager
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        val outputBuffer = replaceOutputBuffer(remainingBytes)
        val isVocalActive = vocalEngineManager.isAudioProcessorActive()

        if (!isVocalActive || !SonoraCppBridge.isAvailable()) {
            // Modo Bypass transparente
            outputBuffer.put(inputBuffer)
        } else {
            val sampleRate = inputAudioFormat.sampleRate
            val channelCount = inputAudioFormat.channelCount

            if (inputBuffer.isDirect && outputBuffer.isDirect) {
                // Copiamos la entrada a la salida y procesamos in-place en C++
                val outPos = outputBuffer.position()
                outputBuffer.put(inputBuffer)

                SonoraCppBridge.nativeProcessDirectVocal(
                    buffer = outputBuffer,
                    offset = outPos,
                    numBytes = remainingBytes,
                    sampleRate = sampleRate,
                    channelCount = channelCount
                )
            } else {
                val shortCount = remainingBytes / 2
                val shortArray = ShortArray(shortCount)
                inputBuffer.asShortBuffer().get(shortArray)

                SonoraCppBridge.nativeProcessVocalPcm(
                    pcmSamples = shortArray,
                    numSamples = shortCount,
                    sampleRate = sampleRate,
                    channelCount = channelCount
                )

                outputBuffer.asShortBuffer().put(shortArray)
                inputBuffer.position(inputBuffer.limit())
                outputBuffer.position(outputBuffer.position() + remainingBytes)
            }
        }

        outputBuffer.flip()
    }

    override fun onFlush() {
        super.onFlush()
        SonoraCppBridge.nativeResetVocalEngine()
    }

    override fun onReset() {
        super.onReset()
        SonoraCppBridge.nativeResetVocalEngine()
    }
}
