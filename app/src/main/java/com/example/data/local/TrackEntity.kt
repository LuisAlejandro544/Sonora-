package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una pista de audio importada por el usuario en Sonora.
 * Almacena los metadatos esenciales, la ruta del archivo y carátula local.
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String = "Artista desconocido",
    val album: String = "Álbum desconocido",
    val durationMs: Long = 0L,
    val filePath: String,
    val albumArtPath: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val mimeType: String? = "audio/mpeg",
    val fileSize: Long = 0L
)
