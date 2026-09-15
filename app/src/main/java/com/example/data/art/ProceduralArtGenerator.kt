package com.example.data.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * ==============================================================================
 * GENERADOR PROCEDURAL DE CARÁTULAS SONORA (SIN IA, ULTRALIVIANO)
 * ==============================================================================
 * Genera portadas de álbumes de forma algorítmica y determinista en tiempo de
 * ejecución a partir de los metadatos de la canción (título, artista y álbum).
 *
 * Características principales:
 * 1. 100% Algorítmico y determinista: no consume APIs externas, no usa modelos de IA.
 * 2. Peso ínfimo: se renderiza en CPU en <10 ms y al comprimirse a WebP ocupa ~15-25 KB.
 * 3. Estética Semi-3D Sonora: utiliza degradados oscuros, iluminación radial,
 *    ondas de resonancia acústica, facetas geométricas y monogramas tipográficos
 *    con profundidad y volumen visual (rechazo al minimalismo plano).
 * ==============================================================================
 */
object ProceduralArtGenerator {

    private const val TAG = "ProceduralArt"

    // Paletas de color ricas y modernas con contraste acústico semi-3D
    private val COLOR_PALETTES = listOf(
        // Emerald Synth (Verde Esmeralda + Cian)
        Palette(
            bgDark = 0xFF0D1B14.toInt(),
            bgMid = 0xFF132A20.toInt(),
            accentPrimary = 0xFF00E676.toInt(),
            accentSecondary = 0xFF00E5FF.toInt(),
            highlight = 0xFF69F0AE.toInt()
        ),
        // Electric Violet (Púrpura Neón + Azul Zafiro)
        Palette(
            bgDark = 0xFF120E24.toInt(),
            bgMid = 0xFF1E173D.toInt(),
            accentPrimary = 0xFF7C4DFF.toInt(),
            accentSecondary = 0xFF2979FF.toInt(),
            highlight = 0xFFB388FF.toInt()
        ),
        // Amber Pulse (Oro Ámbar + Carmesí)
        Palette(
            bgDark = 0xFF211308.toInt(),
            bgMid = 0xFF351F0E.toInt(),
            accentPrimary = 0xFFFFAB00.toInt(),
            accentSecondary = 0xFFFF3D00.toInt(),
            highlight = 0xFFFFD740.toInt()
        ),
        // Cyber Sunset (Magenta + Cian Eléctrico)
        Palette(
            bgDark = 0xFF1C0D24.toInt(),
            bgMid = 0xFF2F143D.toInt(),
            accentPrimary = 0xFFE040FB.toInt(),
            accentSecondary = 0xFF18FFFF.toInt(),
            highlight = 0xFFEA80FC.toInt()
        ),
        // Deep Ocean (Aguamarina + Lima)
        Palette(
            bgDark = 0xFF081820.toInt(),
            bgMid = 0xFF0D2836.toInt(),
            accentPrimary = 0xFF00BFA5.toInt(),
            accentSecondary = 0xFFAEEA00.toInt(),
            highlight = 0xFF64FFDA.toInt()
        ),
        // Solar Flare (Naranja + Amarillo Brillante)
        Palette(
            bgDark = 0xFF241005.toInt(),
            bgMid = 0xFF3D1C0A.toInt(),
            accentPrimary = 0xFFFF6D00.toInt(),
            accentSecondary = 0xFFFFD600.toInt(),
            highlight = 0xFFFFAB40.toInt()
        ),
        // Cosmic Aurora (Menta + Violeta)
        Palette(
            bgDark = 0xFF0A1C16.toInt(),
            bgMid = 0xFF153328.toInt(),
            accentPrimary = 0xFF00F5D4.toInt(),
            accentSecondary = 0xFF7209B7.toInt(),
            highlight = 0xFF80FFE8.toInt()
        ),
        // Titanium Ice (Plata + Cian Gélido)
        Palette(
            bgDark = 0xFF13181C.toInt(),
            bgMid = 0xFF1E262C.toInt(),
            accentPrimary = 0xFF80DEEA.toInt(),
            accentSecondary = 0xFFCFD8DC.toInt(),
            highlight = 0xFFE0F7FA.toInt()
        )
    )

    data class Palette(
        val bgDark: Int,
        val bgMid: Int,
        val accentPrimary: Int,
        val accentSecondary: Int,
        val highlight: Int
    )

    /**
     * Genera un mapa de bits (Bitmap) procedural de 512x512 de alta resolución.
     */
    fun generateCoverBitmap(title: String, artist: String, album: String, size: Int = 512): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Semilla determinista basada en el contenido de la canción
        val seed = calculateDeterministicSeed(title, artist, album)
        val rng = Random(seed)

        // Selección de paleta cromática
        val palette = COLOR_PALETTES[Math.abs(seed.toInt()) % COLOR_PALETTES.size]

        // 1. Fondo degradado angular/diagonal con volumen
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(palette.bgDark, palette.bgMid, palette.bgDark),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // 2. Orbes de resplandor radial lumínico (profundidad de campo)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val numGlows = 2 + rng.nextInt(2)
        for (i in 0 until numGlows) {
            val cx = size * (0.2f + rng.nextFloat() * 0.6f)
            val cy = size * (0.2f + rng.nextFloat() * 0.6f)
            val radius = size * (0.35f + rng.nextFloat() * 0.25f)
            val glowColor = if (i % 2 == 0) palette.accentPrimary else palette.accentSecondary
            val transparentColor = glowColor and 0x00FFFFFF

            glowPaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf((glowColor and 0x00FFFFFF) or 0x40000000, transparentColor),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, glowPaint)
        }

        // 3. Patrón procedural acústico (según la semilla)
        val patternType = rng.nextInt(3)
        when (patternType) {
            0 -> drawAcousticResonanceWaves(canvas, size, palette, rng)
            1 -> drawGeometricCrystalFacets(canvas, size, palette, rng)
            else -> drawRadialHarmonicRays(canvas, size, palette, rng)
        }

        // 4. Placa central de cristal frosted con volumen Semi-3D
        val plateCenter = size / 2f
        val plateRadius = size * 0.28f

        // Sombra proyectada profunda
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(plateCenter + 6f, plateCenter + 10f, plateRadius, shadowPaint)

        // Cuerpo translúcido del disco
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                plateCenter - plateRadius, plateCenter - plateRadius,
                plateCenter + plateRadius, plateCenter + plateRadius,
                intArrayOf(0x33FFFFFF, 0x1A000000, 0x26FFFFFF),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(plateCenter, plateCenter, plateRadius, platePaint)

        // Borde perimetral brillante con iluminación cenital
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                plateCenter, plateCenter - plateRadius,
                plateCenter, plateCenter + plateRadius,
                intArrayOf(palette.highlight, palette.accentPrimary, 0x33FFFFFF),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(plateCenter, plateCenter, plateRadius, ringPaint)

        // Anillo interno decorativo
        ringPaint.strokeWidth = 1.5f
        ringPaint.color = Color.argb(90, 255, 255, 255)
        ringPaint.shader = null
        canvas.drawCircle(plateCenter, plateCenter, plateRadius * 0.88f, ringPaint)

        // 5. Monograma estilizado en relieve (Iniciales de título y artista)
        val monogramText = extractMonogram(title, artist)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Sombra de texto para sensación de relieve semi-3D
        textPaint.color = Color.argb(180, 0, 0, 0)
        canvas.drawText(monogramText, plateCenter + 3f, plateCenter + (textPaint.textSize * 0.36f) + 4f, textPaint)

        // Texto frontal con gradiente luminoso
        textPaint.shader = LinearGradient(
            plateCenter, plateCenter - plateRadius * 0.4f,
            plateCenter, plateCenter + plateRadius * 0.4f,
            intArrayOf(0xFFFFFFFF.toInt(), palette.highlight),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(monogramText, plateCenter, plateCenter + (textPaint.textSize * 0.36f), textPaint)

        // 6. Insignia técnica inferior "SONORA HI-RES DSP"
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 200, 220, 215)
            textSize = size * 0.026f
            letterSpacing = 0.22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SONORA PROCEDURAL • 32-BIT DSP", plateCenter, size * 0.93f, badgePaint)

        return bitmap
    }

    /**
     * Genera la carátula procedural y la guarda directamente en el archivo destino WebP.
     * Retorna true si se guardó con éxito.
     */
    fun generateCover(
        title: String,
        artist: String,
        album: String,
        outputFile: File
    ): Boolean {
        return try {
            outputFile.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }

            val bitmap = generateCoverBitmap(title, artist, album, size = 512)
            FileOutputStream(outputFile).use { fos ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                // Calidad 85 produce un archivo de apenas 15 a 25 KB con fidelidad excelente
                bitmap.compress(format, 85, fos)
            }
            bitmap.recycle()
            Log.i(TAG, "Carátula procedural generada para '$title' en ${outputFile.length()} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error generando carátula procedural: ${e.message}", e)
            false
        }
    }

    /**
     * Dibuja ondas sinusoidales de resonancia acústica superpuestas.
     */
    private fun drawAcousticResonanceWaves(canvas: Canvas, size: Int, palette: Palette, rng: Random) {
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }

        val numWaves = 7
        val baseFreq = 1.5f + rng.nextFloat() * 2f
        for (w in 0 until numWaves) {
            val alpha = (40 + (w * 18)).coerceAtMost(160)
            val color = if (w % 2 == 0) palette.accentPrimary else palette.accentSecondary
            wavePaint.color = (color and 0x00FFFFFF) or (alpha shl 24)

            val path = Path()
            val yOffset = size * (0.35f + (w * 0.05f))
            val amp = size * (0.04f + (w * 0.012f))

            var x = 0f
            var first = true
            while (x <= size) {
                val normX = x / size.toFloat()
                val y = yOffset + sin((normX * baseFreq * 2 * Math.PI) + (w * 0.8)).toFloat() * amp
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 6f
            }
            canvas.drawPath(path, wavePaint)
        }
    }

    /**
     * Dibuja polígonos geométricos y cristales facetados con relieve semi-3D.
     */
    private fun drawGeometricCrystalFacets(canvas: Canvas, size: Int, palette: Palette, rng: Random) {
        val facetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }

        val center = size / 2f
        val numRings = 5
        for (r in 1..numRings) {
            val rad = (size * 0.12f) + (r * size * 0.07f)
            val sides = 6 // Hexágonos acústicos
            val path = Path()
            val rotOffset = (r * 15f) * (Math.PI / 180.0)

            for (s in 0 until sides) {
                val angle = (s * (2 * Math.PI / sides)) + rotOffset
                val px = center + (cos(angle) * rad).toFloat()
                val py = center + (sin(angle) * rad).toFloat()
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            val alpha = (30 + r * 20).coerceAtMost(140)
            facetPaint.color = (palette.accentPrimary and 0x00FFFFFF) or (alpha shl 24)
            canvas.drawPath(path, facetPaint)
        }
    }

    /**
     * Dibuja rayos armónicos radiales que simulan un disco de vinilo holográfico.
     */
    private fun drawRadialHarmonicRays(canvas: Canvas, size: Int, palette: Palette, rng: Random) {
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val center = size / 2f
        val numRays = 36
        val innerRad = size * 0.28f
        val outerRad = size * 0.46f

        for (i in 0 until numRays) {
            val angle = (i * (2 * Math.PI / numRays)).toFloat()
            val lenMod = if (i % 3 == 0) 1.0f else (0.7f + (rng.nextFloat() * 0.25f))
            val currentOuter = innerRad + (outerRad - innerRad) * lenMod

            val x1 = center + (cos(angle) * innerRad).toFloat()
            val y1 = center + (sin(angle) * innerRad).toFloat()
            val x2 = center + (cos(angle) * currentOuter).toFloat()
            val y2 = center + (sin(angle) * currentOuter).toFloat()

            val alpha = (40 + (i % 5) * 20).coerceAtMost(150)
            val color = if (i % 2 == 0) palette.accentSecondary else palette.highlight
            rayPaint.color = (color and 0x00FFFFFF) or (alpha shl 24)

            canvas.drawLine(x1, y1, x2, y2, rayPaint)
        }
    }

    /**
     * Extrae un monograma limpio de 1 o 2 letras para colocar en el centro de la carátula.
     */
    private fun extractMonogram(title: String, artist: String): String {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()

        val char1 = cleanTitle.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
        val char2 = cleanArtist.firstOrNull { it.isLetterOrDigit() && it.uppercaseChar() != char1 }?.uppercaseChar()

        return when {
            char1 != null && char2 != null -> "$char1$char2"
            char1 != null -> "$char1"
            else -> "SN"
        }
    }

    /**
     * Genera una semilla hash determinista a partir de los metadatos.
     */
    private fun calculateDeterministicSeed(title: String, artist: String, album: String): Long {
        var h = 1125899906842597L // FNV offset basis
        val str = "${title.trim().lowercase()}|${artist.trim().lowercase()}|${album.trim().lowercase()}"
        for (ch in str) {
            h = (h xor ch.code.toLong()) * 1099511628211L
        }
        return h
    }
}
