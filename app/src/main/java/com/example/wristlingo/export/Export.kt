package com.example.wristlingo.export

import android.content.Context
import android.net.Uri
import com.example.wristlingo.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun esc(s: String?): String = buildString {
  if (s == null) return@buildString
  s.forEach { c ->
    when (c) {
      '\\' -> append("\\\\")
      '"' -> append("\\\"")
      '\n' -> append("\\n")
      '\r' -> append("\\r")
      '\t' -> append("\\t")
      else -> append(c)
    }
  }
}

suspend fun exportSessions(context: Context, db: AppDatabase, dest: Uri) = withContext(Dispatchers.IO) {
  context.contentResolver.openOutputStream(dest)?.bufferedWriter().use { out ->
    val sessions = db.sessionDao().all()
    for (s in sessions) {
      out?.appendLine("{" +
        "\"type\":\"session\",\"id\":${s.id},\"startedAt\":${s.startedAt},\"endedAt\":${s.endedAt}" +
        "}")
      val utts = db.utteranceDao().bySession(s.id)
      for (u in utts) {
        out?.appendLine("{" +
          "\"type\":\"utterance\",\"id\":${u.id},\"sessionId\":${u.sessionId},\"ts\":${u.ts}," +
          "\"srcText\":\"${esc(u.srcText)}\",\"dstText\":\"${esc(u.dstText)}\",\"lang\":\"${esc(u.lang)}\"" +
          "}")
      }
    }
    out?.flush()
  }
}

