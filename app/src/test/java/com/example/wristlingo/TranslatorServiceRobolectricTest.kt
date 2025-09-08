package com.example.wristlingo

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wristlingo.providers.AsrProvider
import com.example.wristlingo.providers.TranslationProvider
import com.example.wristlingo.service.TranslationPipeline
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class TranslatorServiceRobolectricTest {
  @Test
  fun startStopLifecycle_emitsBusStates() {
    val controller = Robolectric.buildService(TranslatorService::class.java).create()
    val service = controller.get()
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    // Inject a no-op translation provider and a hook to avoid audio hardware
    service.translationProviderFactory = { mockk<TranslationProvider>(relaxed = true) }
    service.processAudioHook = { asr: AsrProvider, _, onFinal ->
      // Simulate one final text and return
      runBlocking { onFinal("hello world") }
    }

    val startIntent = Intent(ctx, TranslatorService::class.java).setAction(TranslatorService.ACTION_START)
    controller.startCommand(0, 0).handleIntent(startIntent)
    // Verify we entered foreground and emitted caption
    assertEquals(true, AppBus.captions.replayCache.isNotEmpty())

    val stopIntent = Intent(ctx, TranslatorService::class.java).setAction(TranslatorService.ACTION_STOP)
    controller.handleIntent(stopIntent)
    controller.destroy()

    assertEquals("idle", AppBus.asrState.value)
  }
}

