package com.example.wristlingo.lang

import android.content.Context
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LanguageId(context: Context) {
  private val id: LanguageIdentifier = LanguageIdentification.getClient()

  suspend fun detect(text: String): String? {
    if (text.isBlank()) return null
    return suspendCancellableCoroutine { cont ->
      id.identifyLanguage(text)
        .addOnSuccessListener { lang -> cont.resume(if (lang == "und") null else lang) }
        .addOnFailureListener { e -> cont.resumeWithException(e) }
    }
  }
}

