package com.example.wristlingo.providers

import kotlinx.coroutines.delay

class FakeTranslationProvider : TranslationProvider {
  override suspend fun ensureModel(source: String?, target: String) {
    // Simulate a tiny model check
    delay(50)
  }

  override suspend fun translate(text: String, source: String?, target: String): String {
    // Cheap “translation”: annotate language and uppercase first letter
    delay(50)
    val normalized = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    return "$normalized [$target]"
  }
}

