package com.example.wristlingo.providers

interface TranslationProvider {
  suspend fun ensureModel(source: String?, target: String)
  suspend fun translate(text: String, source: String?, target: String): String
}

