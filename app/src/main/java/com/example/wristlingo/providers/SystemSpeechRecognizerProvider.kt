package com.example.wristlingo.providers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SystemSpeechRecognizerProvider(private val context: Context) : AsrProvider {
  private var recognizer: SpeechRecognizer? = null
  private var language: String? = null
  private var listener: ((AsrProvider.Partial) -> Unit)? = null

  override fun start(sampleRate: Int, languageHint: String?) {
    language = languageHint
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return
    recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
      setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {}
        override fun onPartialResults(partialResults: Bundle?) {
          val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val text = list?.firstOrNull()
          if (!text.isNullOrBlank()) listener?.invoke(AsrProvider.Partial(text, false))
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onResults(results: Bundle?) {
          val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val text = list?.firstOrNull()
          if (!text.isNullOrBlank()) listener?.invoke(AsrProvider.Partial(text, true))
        }
      })
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        languageHint?.let {
          val tag = Locale.forLanguageTag(it)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
        }
      }
      startListening(intent)
    }
  }

  override fun feedPcm(frame: ShortArray): AsrProvider.Partial? {
    // System recognizer manages mic by itself; ignore external frames
    return null
  }

  override fun finalizeStream(): String {
    recognizer?.stopListening()
    return ""
  }

  override fun close() {
    recognizer?.destroy()
    recognizer = null
  }

  override fun setListener(listener: ((AsrProvider.Partial) -> Unit)?) {
    this.listener = listener
  }
}
