package com.example.wristlingo.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wristlingo.settings.SettingsStore
import com.example.wristlingo.settings.Keys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ServiceConfigurationTest {
  @Test
  fun loadsDefaultsWhenUnset() = runBlocking {
    val context: Context = ApplicationProvider.getApplicationContext()
    val store = SettingsStore(context)
    val cfg = ServiceConfiguration(context).loadConfig(store)
    assertTrue(cfg.targetLang.isNotBlank())
  }

  @Test
  fun validateFlagsOutOfRange() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val config = ServiceConfiguration.Config("fake", "es", redact = false, ttsEnabled = false, autoDetect = true, ttsPitch = 3.0f, ttsRate = 0.1f, ttsVoice = null)
    val issues = ServiceConfiguration(context).validateConfig(config)
    assertTrue(issues.any { it.contains("pitch") })
    assertTrue(issues.any { it.contains("rate") })
  }
}

