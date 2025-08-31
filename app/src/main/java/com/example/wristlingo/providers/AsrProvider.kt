package com.example.wristlingo.providers

import java.io.Closeable

interface AsrProvider : Closeable {
  data class Partial(val text: String, val isFinal: Boolean)
  fun start(sampleRate: Int, languageHint: String? = null)
  fun feedPcm(frame: ShortArray): Partial?
  fun finalizeStream(): String
}

