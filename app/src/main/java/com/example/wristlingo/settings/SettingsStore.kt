package com.example.wristlingo.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wristlingo")

object Keys {
  val provider = stringPreferencesKey("asr_provider") // fake|system
  val targetLang = stringPreferencesKey("target_lang") // e.g., "es"
  val redact = booleanPreferencesKey("redact_pii")
  val tts = booleanPreferencesKey("tts_enabled")
}

class SettingsStore(private val context: Context) {
  val data: Flow<Preferences> = context.dataStore.data

  suspend fun setProvider(id: String) = context.dataStore.edit { it[Keys.provider] = id }
  suspend fun setTargetLang(lang: String) = context.dataStore.edit { it[Keys.targetLang] = lang }
  suspend fun setRedact(enabled: Boolean) = context.dataStore.edit { it[Keys.redact] = enabled }
  suspend fun setTts(enabled: Boolean) = context.dataStore.edit { it[Keys.tts] = enabled }
}

