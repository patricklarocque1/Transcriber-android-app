package com.example.wristlingo.audio

import kotlin.math.abs

class SimpleVad(
  private val speechThreshold: Int = 1500,
  private val minSpeechFrames: Int = 3,   // ~300ms
  private val maxSilenceFrames: Int = 8   // ~800ms
) {
  private var speechFrames = 0
  private var silenceFrames = 0
  private var inSpeech = false

  fun analyze(frame: ShortArray): State {
    val maxAmp = frame.maxOfOrNull { abs(it.toInt()) } ?: 0
    val isSpeech = maxAmp > speechThreshold
    if (isSpeech) {
      speechFrames++
      silenceFrames = 0
    } else {
      silenceFrames++
    }
    if (!inSpeech && speechFrames >= minSpeechFrames) {
      inSpeech = true
      return State.Start
    }
    if (inSpeech && silenceFrames >= maxSilenceFrames) {
      inSpeech = false
      speechFrames = 0
      silenceFrames = 0
      return State.End
    }
    return if (inSpeech) State.Speech else State.Silence
  }

  enum class State { Start, Speech, Silence, End }
}

