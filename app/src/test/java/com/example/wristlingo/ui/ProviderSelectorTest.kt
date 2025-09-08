package com.example.wristlingo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.wristlingo.settings.SettingsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.datastore.preferences.core.Preferences
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class TestSettingsApi : SettingsApi {
  override val data: Flow<Preferences> = emptyFlow()
  var lastProvider: String? = null
  override suspend fun setProvider(id: String) { lastProvider = id }
  override suspend fun setTargetLang(lang: String) {}
  override suspend fun setRedact(enabled: Boolean) {}
  override suspend fun setTts(enabled: Boolean) {}
  override suspend fun setTtsPitch(value: Float) {}
  override suspend fun setTtsRate(value: Float) {}
  override suspend fun setTtsVoice(name: String?) {}
  override suspend fun setAutoLangDetect(enabled: Boolean) {}
}

class ProviderSelectorTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun clickingButtonsInvokesSetProvider() {
    val settings = TestSettingsApi()
    compose.setContent {
      ProviderSelector(currentProvider = "system", store = settings)
    }

    compose.onNodeWithText("Whisper").performClick()
    // Cannot await suspend here easily; just assert lastProvider eventually set
    assertEquals("whisper", settings.lastProvider)

    compose.onNodeWithText("System").performClick()
    assertEquals("system", settings.lastProvider)
  }
}

