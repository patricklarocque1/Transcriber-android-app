package com.example.wristlingo.providers

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitTranslationProvider(private val context: Context) : TranslationProvider {
  private var translator: Translator? = null
  private var current: Pair<String?, String>? = null

  override suspend fun ensureModel(source: String?, target: String) {
    val src = source?.let { toMlKitLang(it) }
    val dst = toMlKitLang(target)
    if (current == Pair(source, target) && translator != null) return
    translator?.close()
    translator = Translation.getClient(
      TranslatorOptions.Builder()
        .setTargetLanguage(dst)
        .apply { if (src != null) setSourceLanguage(src) }
        .build()
    )
    current = Pair(source, target)
    val cond = DownloadConditions.Builder().requireWifi().build()
    awaitTask { translator!!.downloadModelIfNeeded(cond).addOnCompleteListener { it } }
  }

  override suspend fun translate(text: String, source: String?, target: String): String {
    ensureModel(source, target)
    val t = translator ?: return text
    return awaitTask { t.translate(text).addOnCompleteListener { it } }
  }

  private fun toMlKitLang(tag: String): String = TranslateLanguage.fromLanguageTag(tag)
    ?: when (tag.lowercase()) {
      "zh-cn", "zh" -> TranslateLanguage.CHINESE
      else -> throw IllegalArgumentException("Unsupported language: $tag")
    }
}

private suspend fun <T> awaitTask(register: () -> com.google.android.gms.tasks.Task<T>): T =
  suspendCancellableCoroutine { cont ->
    val task = register()
    task.addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    task.addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
  }

