package com.example.wristlingo.providers

import android.content.Context

/**
 * Placeholder for an offline Whisper.cpp JNI-backed provider.
 *
 * Integration steps (not part of this build):
 * 1) Add NDK + CMake and compile a JNI bridge (e.g., libwhisperjni.so) exposing feed/start/finalize.
 * 2) Manage model files under app storage (e.g., context.filesDir/"whisper/models").
 * 3) Stream PCM frames into the native session and emit partial/final results via setListener.
 */
class WhisperCppProvider(private val context: Context) : AsrProvider {
  private var listener: ((AsrProvider.Partial) -> Unit)? = null

  override fun start(sampleRate: Int, languageHint: String?) {
    throw UnsupportedOperationException("Whisper.cpp JNI not included in this build")
  }

  override fun feedPcm(frame: ShortArray): AsrProvider.Partial? = null

  override fun finalizeStream(): String = ""

  override fun setListener(listener: ((AsrProvider.Partial) -> Unit)?) {
    this.listener = listener
  }

  override fun close() {}
}

