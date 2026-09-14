package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Tema principal de Sonora Music Player.
 * Diseñado con una apariencia inmersiva de modo oscuro permanente,
 * ideal para reproductores de audio profesionales.
 *
 * Incluye aislamiento tipográfico propio (fontScale = 1.0f) para asegurar
 * que la aplicación tenga su propio tamaño de letra calibrado, evitando
 * que configuraciones externas del sistema operativo móvil desborden los controles,
 * deslizadores del ecualizador o paneles del reproductor.
 */
private val SonoraDarkColorScheme = darkColorScheme(
    primary = SonoraEmerald,
    onPrimary = Color.Black,
    primaryContainer = SonoraEmeraldDark,
    onPrimaryContainer = SonoraEmeraldBright,
    secondary = SonoraTealAccent,
    onSecondary = Color.Black,
    secondaryContainer = SonoraSurfaceElevated,
    onSecondaryContainer = SonoraTextPrimary,
    tertiary = SonoraEmeraldBright,
    onTertiary = Color.Black,
    background = SonoraBackground,
    onBackground = SonoraTextPrimary,
    surface = SonoraSurface,
    onSurface = SonoraTextPrimary,
    surfaceVariant = SonoraSurfaceElevated,
    onSurfaceVariant = SonoraTextSecondary,
    outline = SonoraSurfaceBorder,
    error = SonoraDanger,
    onError = Color.White
)

@Composable
fun SonoraTheme(
    darkTheme: Boolean = true, // Modo oscuro inmersivo por defecto para reproductor musical
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Aislamiento tipográfico propio: fijamos fontScale = 1.0f respetando la densidad física de pantalla
    val currentDensity = LocalDensity.current
    val customSonoraDensity = Density(
        density = currentDensity.density,
        fontScale = 1.0f
    )

    CompositionLocalProvider(LocalDensity provides customSonoraDensity) {
        MaterialTheme(
            colorScheme = SonoraDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = SonoraTheme(darkTheme, dynamicColor, content)

