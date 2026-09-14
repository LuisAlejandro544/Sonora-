package com.example.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * ==============================================================================
 * COMPRESOR WEBP SIN PÉRDIDA DE CALIDAD (WEBP LOSSLESS)
 * ==============================================================================
 * Convierte carátulas de audio (JPEG / PNG incrustadas en las etiquetas de música)
 * al formato WebP a máxima compresión sin pérdida de calidad (Lossless).
 * 
 * Cumple con el requerimiento de compresión óptima respetando la fidelidad
 * visual de la portada y reduciendo el consumo de almacenamiento en el dispositivo.
 * ==============================================================================
 */
object WebpLosslessCompressor {

    private const val TAG = "WebpCompressor"

    /**
     * Comprime un array de bytes de imagen a formato WebP Lossless y lo guarda en el archivo destino.
     *
     * @param imageBytes Bytes crudos de la imagen extraída de los metadatos de audio.
     * @param destinationFile Archivo .webp de destino donde se guardará la imagen comprimida.
     * @return true si la compresión y guardado fueron exitosos, false en caso de error.
     */
    fun compressToWebpLossless(imageBytes: ByteArray, destinationFile: File): Boolean {
        if (imageBytes.isEmpty()) {
            Log.w(TAG, "Los bytes de imagen están vacíos. No se puede comprimir a WebP.")
            return false
        }

        return try {
            // Decodificación de la imagen en mapa de bits ARGB_8888 para preservar fidelidad total
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPremultiplied = false // Evita alteraciones de canal alfa
            }

            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                ?: run {
                    Log.e(TAG, "No se pudo decodificar el mapa de bits desde los bytes proporcionados.")
                    return false
                }

            // Asegurar que el directorio padre exista
            destinationFile.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }

            // Compresión WebP sin pérdida a máxima fidelidad
            FileOutputStream(destinationFile).use { outputStream ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }

                // Calidad 100 con WEBP_LOSSLESS aplica compresión de diccionario sin perder píxeles
                val success = bitmap.compress(format, 100, outputStream)
                outputStream.flush()
                bitmap.recycle()

                if (success) {
                    Log.d(TAG, "Carátula comprimida a WebP Lossless exitosamente en: ${destinationFile.name} (${destinationFile.length()} bytes)")
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la compresión WebP Lossless: ${e.message}", e)
            false
        }
    }

    /**
     * Comprime un array de bytes y retorna directamente los bytes del WebP resultante en memoria.
     */
    fun compressToWebpBytes(imageBytes: ByteArray): ByteArray? {
        if (imageBytes.isEmpty()) return null
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
            val outputStream = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, 100, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar bytes WebP: ${e.message}")
            null
        }
    }
}
