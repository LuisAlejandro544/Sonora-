package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sonora", appName)
  }

  @Test
  fun `formatDuration formats correctly`() {
    assertEquals("0:00", formatDuration(0L))
    assertEquals("3:45", formatDuration(225000L))
    assertEquals("1:05:30", formatDuration(3930000L))
  }

  @Test
  fun `formatFileSize formats correctly`() {
    assertEquals("0 MB", formatFileSize(0L))
    assertEquals("5.2 MB", formatFileSize((5.2 * 1024 * 1024).toLong()))
  }
}

