package com.example.wristlingo.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wristlingo")

object Keys {
  val provider = stringPreferencesKey("asr_provider") // fake|system
  val targetLang = stringPreferencesKey("target_lang") // e.g., "es"
  val redact = booleanPreferencesKey("redact_pii")
  val tts = booleanPreferencesKey("tts_enabled")
  val ttsPitch = floatPreferencesKey("tts_pitch") // 0.5..2.0, default 1.0
  val ttsRate = floatPreferencesKey("tts_rate")   // 0.5..2.0, default 1.0
  val ttsVoice = stringPreferencesKey("tts_voice") // engine-specific voice name
  val autoLangDetect = booleanPreferencesKey("auto_lang_detect")
}

class SettingsStore(private val context: Context) {
  val data: Flow<Preferences> = context.dataStore.data

  suspend fun setProvider(id: String) = context.dataStore.edit { it[Keys.provider] = id }
  suspend fun setTargetLang(lang: String) = context.dataStore.edit { it[Keys.targetLang] = lang }
  suspend fun setRedact(enabled: Boolean) = context.dataStore.edit { it[Keys.redact] = enabled }
  suspend fun setTts(enabled: Boolean) = context.dataStore.edit { it[Keys.tts] = enabled }
  suspend fun setTtsPitch(value: Float) = context.dataStore.edit { it[Keys.ttsPitch] = value }
  suspend fun setTtsRate(value: Float) = context.dataStore.edit { it[Keys.ttsRate] = value }
  suspend fun setTtsVoice(name: String?) = context.dataStore.edit { if (name == null) it.remove(Keys.ttsVoice) else it[Keys.ttsVoice] = name }
  suspend fun setAutoLangDetect(enabled: Boolean) = context.dataStore.edit { it[Keys.autoLangDetect] = enabled }
}
