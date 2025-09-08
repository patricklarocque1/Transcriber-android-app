package com.example.wristlingo.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleVadTest {
  @Test
  fun detectsStartSpeechAndEnd() {
    val vad = SimpleVad(speechThreshold = 10, minSpeechFrames = 2, maxSilenceFrames = 2)
    val silence = ShortArray(1600) { 1 }
    val speech = ShortArray(1600) { 100 }

    // Initial silence
    assertEquals(SimpleVad.State.Silence, vad.analyze(silence))
    // Enter speech
    assertEquals(SimpleVad.State.Silence, vad.analyze(speech))
    assertEquals(SimpleVad.State.Start, vad.analyze(speech))
    assertEquals(SimpleVad.State.Speech, vad.analyze(speech))
    // End after enough silence
    assertEquals(SimpleVad.State.Speech, vad.analyze(silence))
    assertEquals(SimpleVad.State.End, vad.analyze(silence))
    assertEquals(SimpleVad.State.Silence, vad.analyze(silence))
  }
}

