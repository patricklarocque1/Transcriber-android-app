package com.example.wristlingo.whisper

import android.content.Context
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object WhisperModelManager {
  private const val TINY_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin?download=true"
  private const val FILE_NAME = "ggml-tiny.bin"

  fun modelDir(context: Context): File = File(context.filesDir, "whisper/models").apply { mkdirs() }
  fun modelFile(context: Context): File = File(modelDir(context), FILE_NAME)
  fun isPresent(context: Context): Boolean = modelFile(context).exists()

  suspend fun downloadTiny(context: Context, progress: ((Long, Long?) -> Unit)? = null) {
    val client = OkHttpClient()
    val req = Request.Builder().url(TINY_URL).build()
    val resp = client.newCall(req).execute()
    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${'$'}{resp.code}")
    val body = resp.body ?: throw IllegalStateException("empty body")
    modelDir(context)
    modelFile(context).outputStream().use { out ->
      body.byteStream().use { ins ->
        val buf = ByteArray(8192)
        var read: Int
        var total = 0L
        val len = body.contentLength().takeIf { it > 0 }
        while (true) {
          read = ins.read(buf)
          if (read <= 0) break
          out.write(buf, 0, read)
          total += read
          progress?.invoke(total, len)
        }
      }
    }
  }
}

object WhisperUiActions {
  suspend fun downloadTinyModel(context: Context) {
    try {
      Toast.makeText(context, "Downloading Whisper Tiny…", Toast.LENGTH_SHORT).show()
      WhisperModelManager.downloadTiny(context) { bytes, total -> }
      Toast.makeText(context, "Whisper Tiny downloaded", Toast.LENGTH_SHORT).show()
    } catch (t: Throwable) {
      Toast.makeText(context, "Download failed: ${'$'}{t.message}", Toast.LENGTH_LONG).show()
    }
  }
}

