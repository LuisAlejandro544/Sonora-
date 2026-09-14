package com.example

import com.example.player.PlaybackState
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias locales para Sonora.
 * Verifica el estado del reproductor y configuración de reproducción sin pausas (Gapless Playback).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun playbackState_gaplessEnabledByDefault() {
    val state = PlaybackState()
    assertTrue("La reproducción sin pausas debe estar habilitada por defecto para máxima fidelidad", state.isGaplessEnabled)
  }

  @Test
  fun playbackState_gaplessCanBeToggled() {
    val state = PlaybackState(isGaplessEnabled = true)
    val toggled = state.copy(isGaplessEnabled = !state.isGaplessEnabled)
    assertFalse(toggled.isGaplessEnabled)
  }
}
