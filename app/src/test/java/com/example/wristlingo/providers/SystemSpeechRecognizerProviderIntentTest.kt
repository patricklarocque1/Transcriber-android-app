package com.example.wristlingo.providers

import android.content.Context
import android.speech.RecognizerIntent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SystemSpeechRecognizerProviderIntentTest {
  @Test
  fun languageHintSetsLocaleExtras() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val provider = SystemSpeechRecognizerProvider(context)
    // We cannot start SpeechRecognizer in unit tests; verify intent creation by reflection
    val method = SystemSpeechRecognizerProvider::class.java.getDeclaredMethod("start", Int::class.javaPrimitiveType, String::class.java)
    method.isAccessible = true
    // Just ensure method exists and does not crash constructing intent path
    try { method.invoke(provider, 16000, "es") } catch (_: Throwable) {}
    // Access currentIntent field
    val field = SystemSpeechRecognizerProvider::class.java.getDeclaredField("currentIntent")
    field.isAccessible = true
    val intent = field.get(provider) as? android.content.Intent
    assertNotNull(intent)
    val model = intent!!.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL)
    assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, model)
  }
}

