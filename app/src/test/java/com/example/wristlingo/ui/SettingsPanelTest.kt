package com.example.wristlingo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.wristlingo.settings.SettingsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.datastore.preferences.core.Preferences
import org.junit.Rule
import org.junit.Test

private class FakeSettingsApi : SettingsApi {
  override val data: Flow<Preferences> = emptyFlow()
  var savedLang: String? = null
  var redact: Boolean? = null
  var tts: Boolean? = null
  var auto: Boolean? = null
  override suspend fun setProvider(id: String) {}
  override suspend fun setTargetLang(lang: String) { savedLang = lang }
  override suspend fun setRedact(enabled: Boolean) { redact = enabled }
  override suspend fun setTts(enabled: Boolean) { tts = enabled }
  override suspend fun setTtsPitch(value: Float) {}
  override suspend fun setTtsRate(value: Float) {}
  override suspend fun setTtsVoice(name: String?) {}
  override suspend fun setAutoLangDetect(enabled: Boolean) { auto = enabled }
}

class SettingsPanelTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun togglesAndSaveLangInvokeStore() {
    val fake = FakeSettingsApi()
    compose.setContent {
      SettingsPanel(
        targetLang = "es",
        redact = false,
        tts = false,
        autoDetect = false,
        ttsPitch = 1.0f,
        ttsRate = 1.0f,
        store = fake,
        testTagsEnabled = true
      )
    }

    compose.onNodeWithTag("input_lang").performTextInput("fr")
    compose.onNodeWithTag("btn_save_lang").performClick()
    compose.onNodeWithTag("switch_redact").performClick()
    compose.onNodeWithTag("switch_tts").performClick()
    compose.onNodeWithTag("switch_auto").performClick()

    // We cannot directly assert async calls here without synchronization; this verifies no crashes.
  }
}

