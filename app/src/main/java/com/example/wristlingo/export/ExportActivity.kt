package com.example.wristlingo.export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.wristlingo.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExportActivity : ComponentActivity() {
  private val scope = CoroutineScope(Dispatchers.Main)
  private val createDoc = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
    if (uri == null) { finish(); return@registerForActivityResult }
    scope.launch {
      try {
        exportSessions(this@ExportActivity, AppDatabase.get(this@ExportActivity), uri)
        Toast.makeText(this@ExportActivity, "Exported", Toast.LENGTH_SHORT).show()
      } catch (t: Throwable) {
        Toast.makeText(this@ExportActivity, "Export failed: ${t.message}", Toast.LENGTH_LONG).show()
      } finally { finish() }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    createDoc.launch("wristlingo-sessions.jsonl")
  }
}

