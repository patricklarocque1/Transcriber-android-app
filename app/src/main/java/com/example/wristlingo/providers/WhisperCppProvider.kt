package com.example.wristlingo.providers

import android.content.Context
import com.example.wristlingo.whisper.WhisperModelManager

class WhisperCppProvider(private val context: Context) : AsrProvider {
  private var listener: ((AsrProvider.Partial) -> Unit)? = null
  private var handle: Long = 0
  private var sampleRate: Int = 16000

  init { System.loadLibrary("whisperjni") }

  override fun start(sampleRate: Int, languageHint: String?) {
    this.sampleRate = sampleRate
    val model = WhisperModelManager.modelFile(context)
    if (!model.exists()) throw IllegalStateException("Whisper model missing")
    handle = WhisperCppNative.nativeInit(model.absolutePath, sampleRate)
  }

  override fun feedPcm(frame: ShortArray): AsrProvider.Partial? {
    if (handle == 0L) return null
    WhisperCppNative.nativeFeed(handle, frame)
    return null
  }

  override fun finalizeStream(): String {
    if (handle == 0L) return ""
    return WhisperCppNative.nativeFinalize(handle)
  }

  override fun setListener(listener: ((AsrProvider.Partial) -> Unit)?) { this.listener = listener }

  override fun close() { handle = 0 }
}

object WhisperCppNative {
  external fun nativeInit(modelPath: String, sampleRate: Int): Long
  external fun nativeFeed(handle: Long, pcm: ShortArray)
  external fun nativeFinalize(handle: Long): String
}
