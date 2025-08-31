package com.example.wristlingo.providers
import com.example.wristlingo.AppBus

class FakeAsrProvider : AsrProvider {
  private var started = false
  private var frames = 0
  private var partialIdx = 0
  private var listener: ((AsrProvider.Partial) -> Unit)? = null
  private val steps = listOf(
    "hello",
    "hello world",
    "hello world from",
    "hello world from wristlingo"
  )

  override fun start(sampleRate: Int, languageHint: String?) {
    started = true
    frames = 0
    partialIdx = 0
    AppBus.asrState.value = "ready"
  }

  override fun feedPcm(frame: ShortArray): AsrProvider.Partial? {
    if (!started) return null
    frames++
    // Emit a partial roughly every 2 frames to simulate streaming ASR
    return if (frames % 2 == 0 && partialIdx < steps.size) {
      val text = steps[partialIdx]
      partialIdx++
      val isFinal = partialIdx >= steps.size
      if (partialIdx == 1) AppBus.asrState.value = "listening"
      if (isFinal) AppBus.asrState.value = "processing"
      val p = AsrProvider.Partial(text, isFinal)
      listener?.invoke(p)
      p
    } else null
  }

  override fun finalizeStream(): String {
    started = false
    val out = steps.last()
    AppBus.asrState.value = "idle"
    return out
  }

  override fun close() { started = false; AppBus.asrState.value = "idle" }

  override fun setListener(listener: ((AsrProvider.Partial) -> Unit)?) {
    this.listener = listener
  }
}
