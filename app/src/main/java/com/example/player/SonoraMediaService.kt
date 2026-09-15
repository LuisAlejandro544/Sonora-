package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.R

/**
 * Servicio en primer plano de Sonora basado en Media3 MediaSessionService.
 *
 * Mantiene la reproducción activa cuando el usuario sale de la aplicación, minimiza la pantalla
 * o bloquea el dispositivo, emitiendo la notificación multimedia interactiva del sistema con:
 * - Carátula del álbum en alta definición (cargada desde WebP sin pérdida o arte procedural).
 * - Título de la pista y artista en tiempo real.
 * - Controles de transporte: Reproducir / Pausar, Pista anterior y Pista siguiente.
 * - Barra scrubber de navegación y compatibilidad con el reproductor nativo de Android 13+.
 */
class SonoraMediaService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "sonora_playback_channel"
        const val NOTIFICATION_ID = 1001
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()

        // Configuración del proveedor de notificación nativa de Media3
        val notificationProvider = DefaultMediaNotificationProvider.Builder(applicationContext)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.playback_notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build().apply {
                setSmallIcon(R.drawable.ic_notification_music)
            }

        setMediaNotificationProvider(notificationProvider)
    }

    /**
     * Devuelve la sesión de MediaSession vinculada al ExoPlayer activo.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val playbackManager = PlaybackManager.getInstance(applicationContext)
        return playbackManager.mediaSession
    }

    /**
     * Cuando la app se elimina de la lista de tareas recientes (deslizar para cerrar):
     * Si la música no está sonando activamente, detenemos el servicio para ahorrar batería.
     * Si la música está sonando, el servicio de primer plano continuará la reproducción sin interrupción.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val playbackManager = PlaybackManager.getInstance(applicationContext)
        if (!playbackManager.isPlaying()) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Registra el canal de notificación para Android 8.0 Oreo (API 26) o superior.
     * Utiliza prioridad LOW para permitir la barra de scrubber sin emitir pitidos en cada canción.
     */
    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.playback_notification_channel_name)
            val channelDesc = getString(R.string.playback_notification_channel_description)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDesc
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
