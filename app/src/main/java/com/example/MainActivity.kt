package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sonora.nativeengine.NativeEngineManager
import com.example.ui.MainScreen
import com.example.ui.theme.SonoraTheme

/**
 * Actividad principal de la aplicación Sonora.
 * Configura el modo Edge-to-Edge e inicia el árbol de interfaz en Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Precargar motores nativos en segundo plano
        NativeEngineManager.getStatus()
        enableEdgeToEdge()
        setContent {
            SonoraTheme {
                MainScreen()
            }
        }
    }
}

