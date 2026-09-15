package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.TrackEntity
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraEmeraldBright
import com.example.ui.theme.SonoraEmeraldDark
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHighlight
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

/**
 * Diálogo flotante semi-3D para la edición de metadatos de una canción en Sonora.
 * Permite al usuario modificar en tiempo real el título, artista y álbum del archivo de audio.
 */
@Composable
fun EditMetadataDialog(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }
    var isTitleError by remember { mutableStateOf(false) }
    var rustCleanMessage by remember { mutableStateOf<String?>(null) }
    var isRustCleaned by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SonoraSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        SonoraEmeraldBright.copy(alpha = 0.45f),
                        SonoraSurfaceHighlight.copy(alpha = 0.2f)
                    )
                )
            ),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = SonoraEmeraldBright.copy(alpha = 0.35f)
                )
                .testTag("edit_metadata_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Cabecera del diálogo con icono estilizado y botón de cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(SonoraEmeraldDark, SonoraEmerald)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = SonoraEmeraldBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Editar Metadatos",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                color = SonoraTextPrimary
                            )
                            Text(
                                text = "Personaliza la información de tu audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_edit_metadata_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar",
                            tint = SonoraTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botón de Saneamiento y Limpieza Inteligente con Rust
                Button(
                    onClick = {
                        val result = com.example.sonora.nativeengine.SonoraRustBridge.sanitizeMetadataSafe(
                            title = title,
                            artist = artist,
                            album = album,
                            filePath = track.filePath
                        )
                        if (result != null) {
                            title = result.title
                            artist = result.artist
                            album = result.album
                            isTitleError = false
                            if (result.wasModified) {
                                isRustCleaned = true
                                rustCleanMessage = "¡Limpiado con Rust! Se corrigieron caracteres y etiquetas."
                            } else {
                                isRustCleaned = false
                                rustCleanMessage = "Los metadatos ya están limpios y en formato óptimo."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonoraSurfaceElevated,
                        contentColor = SonoraEmeraldBright
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRustCleaned) SonoraEmeraldBright else SonoraSurfaceHighlight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rust_clean_metadata_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRustCleaned) Icons.Default.Check else Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = SonoraEmeraldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRustCleaned) "Limpieza completada con Rust" else "Limpiar nombres y metadatos con Rust",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = SonoraTextPrimary
                        )
                    }
                }

                if (rustCleanMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = rustCleanMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isRustCleaned) SonoraEmeraldBright else SonoraTextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo 1: Título de la pista (Nombre de la canción)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        isTitleError = it.isBlank()
                    },
                    label = { Text("Nombre de la canción") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isTitleError) MaterialTheme.colorScheme.error else SonoraEmeraldBright
                        )
                    },
                    isError = isTitleError,
                    supportingText = {
                        if (isTitleError) {
                            Text("El título no puede estar en blanco", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SonoraSurfaceElevated,
                        unfocusedContainerColor = SonoraSurfaceElevated,
                        focusedBorderColor = SonoraEmeraldBright,
                        unfocusedBorderColor = SonoraSurfaceHighlight,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary,
                        focusedLabelColor = SonoraEmeraldBright,
                        unfocusedLabelColor = SonoraTextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Campo 2: Artista
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Nombre del artista") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SonoraEmeraldBright
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SonoraSurfaceElevated,
                        unfocusedContainerColor = SonoraSurfaceElevated,
                        focusedBorderColor = SonoraEmeraldBright,
                        unfocusedBorderColor = SonoraSurfaceHighlight,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary,
                        focusedLabelColor = SonoraEmeraldBright,
                        unfocusedLabelColor = SonoraTextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_artist_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Campo 3: Álbum
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Nombre del álbum") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = SonoraEmeraldBright
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SonoraSurfaceElevated,
                        unfocusedContainerColor = SonoraSurfaceElevated,
                        focusedBorderColor = SonoraEmeraldBright,
                        unfocusedBorderColor = SonoraSurfaceHighlight,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary,
                        focusedLabelColor = SonoraEmeraldBright,
                        unfocusedLabelColor = SonoraTextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_album_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Fila de botones de acción con dimensiones ergonómicas mínimas de 48dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_edit_metadata_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraTextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SonoraSurfaceHighlight)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title.trim(), artist.trim(), album.trim())
                            } else {
                                isTitleError = true
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("save_metadata_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonoraEmerald,
                            contentColor = Color.Black,
                            disabledContainerColor = SonoraSurfaceHighlight,
                            disabledContentColor = SonoraTextMuted
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Guardar cambios",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
