package com.example.wristlingo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import com.example.wristlingo.settings.Keys
import com.example.wristlingo.settings.SettingsStore
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.export.exportSessions
import com.example.wristlingo.tts.VoicePicker
import com.example.wristlingo.whisper.WhisperUiActions
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Surface(color = MaterialTheme.colorScheme.background) {
        HomeScreen()
      }
    }
  }
}

@Composable
private fun HomeScreen() {
  val context = LocalContext.current
  var caption by remember { mutableStateOf("Idle – press Start") }
  val store = remember { SettingsStore(context) }
  val scope = rememberCoroutineScope()
  val prefs by store.data.collectAsState(initial = null)
  val provider = (prefs?.get(Keys.provider) ?: "fake")
  val targetLangPref = (prefs?.get(Keys.targetLang) ?: "es")
  val redact = (prefs?.get(Keys.redact) ?: false)
  val tts = (prefs?.get(Keys.tts) ?: false)
  val pitchPref = (prefs?.get(Keys.ttsPitch) ?: 1.0f)
  val ratePref = (prefs?.get(Keys.ttsRate) ?: 1.0f)
  val autoDetect = (prefs?.get(Keys.autoLangDetect) ?: true)
  val savedVoice = prefs?.get(Keys.ttsVoice)
  val db = remember { AppDatabase.get(context) }

  // Observe caption updates from the service via AppBus
  LaunchedEffect(Unit) {
    AppBus.captions.collectLatest { caption = it }
  }
  val asrState by AppBus.asrState.collectAsState()

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      Toast.makeText(context, "Notifications allowed", Toast.LENGTH_SHORT).show()
      startService(context, TranslatorService.ACTION_START)
    } else {
      Toast.makeText(context, "Notifications denied", Toast.LENGTH_SHORT).show()
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) startService(context, TranslatorService.ACTION_START)
    else Toast.makeText(context, "Mic permission denied", Toast.LENGTH_SHORT).show()
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "WristLingo\n$caption",
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleMedium
    )
    // ASR state indicator
    val color = when (asrState) {
      "ready" -> Color(0xFFFBC02D)
      "listening" -> Color(0xFF2E7D32)
      "processing" -> Color(0xFF0288D1)
      "error" -> Color(0xFFC62828)
      else -> Color(0xFF9E9E9E)
    }
    val scale = if (asrState == "listening") {
      val t = rememberInfiniteTransition(label = "asr-pulse")
      t.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "asr-pulse-scale"
      ).value
    } else 1f
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.size(10.dp).scale(scale).clip(CircleShape).background(color))
      Spacer(Modifier.width(8.dp))
      Text("ASR: ${asrState.replaceFirstChar { it.uppercase() }}")
    }
    // Provider selection
    Text("ASR Provider: $provider")
    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedButton(onClick = { scope.launch { store.setProvider("fake") } }) { Text("Fake") }
      Spacer(Modifier.width(8.dp))
      OutlinedButton(onClick = { scope.launch { store.setProvider("system") } }) { Text("System") }
      Spacer(Modifier.width(8.dp))
      OutlinedButton(onClick = { scope.launch {
        store.setProvider("whisper");
        Toast.makeText(context, "Whisper provider not available in this build", Toast.LENGTH_SHORT).show()
      } }) { Text("Whisper (stub)") }
    }
    // Target language
    var lang by remember(targetLangPref) { mutableStateOf(targetLangPref) }
    OutlinedTextField(value = lang, onValueChange = { lang = it }, label = { Text("Target lang (e.g., es)") })
    OutlinedButton(onClick = { scope.launch { store.setTargetLang(lang) } }) { Text("Save Lang") }
    // Toggles
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Redact PII")
      Spacer(Modifier.width(8.dp))
      Switch(checked = redact, onCheckedChange = { scope.launch { store.setRedact(it) } })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("TTS")
      Spacer(Modifier.width(8.dp))
      Switch(checked = tts, onCheckedChange = { scope.launch { store.setTts(it) } })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Auto Lang Detect")
      Spacer(Modifier.width(8.dp))
      Switch(checked = autoDetect, onCheckedChange = { scope.launch { store.setAutoLangDetect(it) } })
    }
    // TTS Pitch and Rate sliders
    var pitch by remember(pitchPref) { mutableStateOf(pitchPref) }
    Text("Pitch: ${String.format("%.2f", pitch)}")
    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..1.5f)
    OutlinedButton(onClick = { scope.launch { store.setTtsPitch(pitch) } }) { Text("Save Pitch") }
    var rate by remember(ratePref) { mutableStateOf(ratePref) }
    Text("Rate: ${String.format("%.2f", rate)}")
    Slider(value = rate, onValueChange = { rate = it }, valueRange = 0.5f..1.5f)
    OutlinedButton(onClick = { scope.launch { store.setTtsRate(rate) } }) { Text("Save Rate") }

    // Voice picker (by target language)
    VoicePicker(targetLangPref, savedVoice) { chosen -> scope.launch { store.setTtsVoice(chosen) } }

    if (provider == "whisper") {
      OutlinedButton(onClick = { scope.launch { WhisperUiActions.downloadTinyModel(context) } }) { Text("Download Whisper Tiny model") }
    }
    Button(onClick = {
      if (Build.VERSION.SDK_INT >= 33) {
        val granted = ContextCompat.checkSelfPermission(
          context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
          notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          return@Button
        }
      }
      if (provider == "system") {
        val micGranted = ContextCompat.checkSelfPermission(
          context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
          micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          return@Button
        }
      }
      Toast.makeText(context, "Starting…", Toast.LENGTH_SHORT).show()
      startService(context, TranslatorService.ACTION_START)
    }) { Text("Start") }

    Button(onClick = {
      Toast.makeText(context, "Stopping…", Toast.LENGTH_SHORT).show()
      startService(context, TranslatorService.ACTION_STOP)
    }) {
      Text("Stop")
    }

    // Export sessions (JSONL via SAF)
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/json")) { uri: Uri? ->
      if (uri != null) scope.launch { exportSessions(context, db, uri) }
    }
    OutlinedButton(onClick = { exportLauncher.launch("wristlingo-sessions.jsonl") }) { Text("Export Sessions") }
  }
}

private fun startService(context: android.content.Context, action: String) {
  val intent = Intent(context, TranslatorService::class.java).setAction(action)
  val isStart = action == TranslatorService.ACTION_START
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isStart) {
    // Use FGS only when starting the service
    ContextCompat.startForegroundService(context, intent)
  } else {
    // For STOP or pre-O just start a normal service to deliver the intent
    context.startService(intent)
  }
}

@Preview
@Composable
private fun PreviewHome() {
  HomeScreen()
}
