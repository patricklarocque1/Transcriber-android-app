package com.example.wristlingo.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice as TtsVoice
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun VoicePicker(targetLang: String, savedVoice: String?, onChoose: (String?) -> Unit) {
  val context = LocalContext.current
  var voices by remember { mutableStateOf<List<TtsVoice>>(emptyList()) }
  var expanded by remember { mutableStateOf(false) }

  DisposableEffect(targetLang, context) {
    var engineRef: TextToSpeech? = null
    engineRef = TextToSpeech(context) { status ->
      val eng = engineRef
      if (status == TextToSpeech.SUCCESS && eng != null) {
        val set: Set<TtsVoice> = eng.voices ?: emptySet()
        val list: List<TtsVoice> = set
          .filter { v: TtsVoice ->
            val tag = v.locale?.toLanguageTag()?.lowercase(Locale.ROOT) ?: ""
            tag.startsWith(targetLang.lowercase(Locale.ROOT))
          }
          .sortedBy { it.name }
        voices = list
      }
    }
    onDispose { engineRef?.shutdown() }
  }

  Row {
    OutlinedButton(onClick = { expanded = true }) { Text(text = savedVoice ?: "Choose TTS Voice") }
    Spacer(Modifier.width(8.dp))
    OutlinedButton(onClick = { onChoose(null) }) { Text("System Default") }
  }
  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
    voices.forEach { v: TtsVoice ->
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
