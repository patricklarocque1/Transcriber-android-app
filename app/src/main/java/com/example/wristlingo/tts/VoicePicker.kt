package com.example.wristlingo.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.ui.platform.LocalContext

@Composable
fun VoicePicker(targetLang: String, savedVoice: String?, onChoose: (String?) -> Unit) {
  var tts by remember { mutableStateOf<TextToSpeech?>(null) }
  var voices by remember { mutableStateOf(emptyList<TextToSpeech.Voice>()) }
  var expanded by remember { mutableStateOf(false) }

  DisposableEffect(targetLang) {
    val engine = TextToSpeech(LocalContext.current) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val list = engine.voices?.filter { v ->
          val tag = v.locale?.toLanguageTag()?.lowercase(Locale.ROOT) ?: ""
          tag.startsWith(targetLang.lowercase(Locale.ROOT))
        }?.sortedBy { it.name } ?: emptyList()
        voices = list
      }
    }
    tts = engine
    onDispose { engine.shutdown() }
  }

  Row {
    OutlinedButton(onClick = { expanded = true }) { Text(text = savedVoice ?: "Choose TTS Voice") }
    Spacer(Modifier.width(8.dp))
    OutlinedButton(onClick = { onChoose(null) }) { Text("System Default") }
  }
  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
    voices.forEach { v ->
      DropdownMenuItem(
        text = { Text(v.name) },
        onClick = { onChoose(v.name); expanded = false }
      )
    }
    if (voices.isEmpty()) {
      DropdownMenuItem(text = { Text("No voices for $targetLang") }, onClick = { expanded = false })
    }
  }
}
