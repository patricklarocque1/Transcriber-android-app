package com.example.wristlingo.export

import android.content.Context
import android.net.Uri
import com.example.wristlingo.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Writer

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

internal suspend fun exportSessionsToWriter(db: AppDatabase, writer: Writer) = withContext(Dispatchers.IO) {
  val sessions = db.sessionDao().all()
  for (s in sessions) {
    writer.appendLine("{" +
      "\"type\":\"session\",\"id\":${s.id},\"startedAt\":${s.startedAt},\"endedAt\":${s.endedAt}" +
      "}")
    val utts = db.utteranceDao().bySession(s.id)
    for (u in utts) {
      writer.appendLine("{" +
        "\"type\":\"utterance\",\"id\":${u.id},\"sessionId\":${u.sessionId},\"ts\":${u.ts}," +
        "\"srcText\":\"${esc(u.srcText)}\",\"dstText\":\"${esc(u.dstText)}\",\"lang\":\"${esc(u.lang)}\"" +
        "}")
    }
  }
  writer.flush()
}

suspend fun exportSessions(context: Context, db: AppDatabase, dest: Uri) = withContext(Dispatchers.IO) {
  context.contentResolver.openOutputStream(dest)?.bufferedWriter().use { out ->
    if (out != null) exportSessionsToWriter(db, out)
  }
}

