package com.example.wristlingo.providers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import com.example.wristlingo.AppBus

class SystemSpeechRecognizerProvider(private val context: Context) : AsrProvider {
  private var recognizer: SpeechRecognizer? = null
  private var language: String? = null
  private var listener: ((AsrProvider.Partial) -> Unit)? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private var running = false
  private var currentIntent: Intent? = null

  override fun start(sampleRate: Int, languageHint: String?) {
    language = languageHint
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return
    running = true
    runOnMain {
      AppBus.asrState.value = "starting"
      recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) { AppBus.asrState.value = "ready" }
          override fun onBeginningOfSpeech() { AppBus.asrState.value = "listening" }
          override fun onRmsChanged(rmsdB: Float) {}
          override fun onBufferReceived(buffer: ByteArray?) {}
          override fun onEndOfSpeech() { AppBus.asrState.value = "processing" }
          override fun onError(error: Int) {
            AppBus.asrState.value = "error"
            // Attempt to recover by restarting listening shortly after
            if (running) {
              mainHandler.postDelayed({ restartListening() }, 350)
            }
          }
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
            // Re-arm for continuous recognition
            if (running) {
              AppBus.asrState.value = "ready"
              restartListening()
            } else {
              AppBus.asrState.value = "idle"
            }
          }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
          putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
          putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800)
          putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600)
          languageHint?.let {
            val tag = Locale.forLanguageTag(it)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
          }
        }
        currentIntent = intent
        startListening(intent)
      }
    }
  }

  override fun feedPcm(frame: ShortArray): AsrProvider.Partial? = null // Ignored for system ASR

  override fun finalizeStream(): String {
    running = false
    runOnMain { recognizer?.stopListening() }
    AppBus.asrState.value = "idle"
    return ""
  }

  override fun close() {
    running = false
    runOnMain {
      recognizer?.destroy()
      recognizer = null
    }
    AppBus.asrState.value = "idle"
  }

  override fun setListener(listener: ((AsrProvider.Partial) -> Unit)?) { this.listener = listener }

  private fun runOnMain(block: () -> Unit) {
    if (Looper.getMainLooper().thread == Thread.currentThread()) block() else mainHandler.post(block)
  }

  private fun restartListening() {
    val r = recognizer ?: return
    val intent = currentIntent ?: return
    try {
      r.cancel()
    } catch (_: Throwable) {}
    try {
      r.startListening(intent)
    } catch (_: Throwable) {}
  }
}
