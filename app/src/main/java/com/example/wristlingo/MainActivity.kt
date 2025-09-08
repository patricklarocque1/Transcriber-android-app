package com.example.wristlingo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.export.exportSessions
import com.example.wristlingo.settings.SettingsStore
import com.example.wristlingo.settings.SettingsApi
import com.example.wristlingo.tts.VoicePicker
import com.example.wristlingo.ui.*
import com.example.wristlingo.whisper.WhisperUiActions
import androidx.compose.animation.core.*

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
  val store: SettingsApi = remember { SettingsStore(context) }
  val scope = rememberCoroutineScope()
  val prefs by store.data.collectAsState(initial = null)
  
  // Extract preferences with defaults
  val provider = prefs?.get(com.example.wristlingo.settings.Keys.provider) ?: "fake"
  val targetLangPref = prefs?.get(com.example.wristlingo.settings.Keys.targetLang) ?: "es"
  val redact = prefs?.get(com.example.wristlingo.settings.Keys.redact) ?: false
  val tts = prefs?.get(com.example.wristlingo.settings.Keys.tts) ?: false
  val pitchPref = prefs?.get(com.example.wristlingo.settings.Keys.ttsPitch) ?: 1.0f
  val ratePref = prefs?.get(com.example.wristlingo.settings.Keys.ttsRate) ?: 1.0f
  val autoDetect = prefs?.get(com.example.wristlingo.settings.Keys.autoLangDetect) ?: true
  val savedVoice = prefs?.get(com.example.wristlingo.settings.Keys.ttsVoice)
  
  val db = remember { AppDatabase.get(context) }

  // Observe caption updates from the service via AppBus
  LaunchedEffect(Unit) {
    AppBus.captions.collectLatest { caption = it }
  }
  val asrState by AppBus.asrState.collectAsState()

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header and status
    StatusHeader(caption = caption, asrState = asrState)
    
    // Provider selection
    ProviderSelector(
      currentProvider = provider,
      store = store
    )
    
    // Settings panel
    SettingsPanel(
      targetLang = targetLangPref,
      redact = redact,
      tts = tts,
      autoDetect = autoDetect,
      ttsPitch = pitchPref,
      ttsRate = ratePref,
      store = store
    )

    // Voice picker
    VoicePicker(targetLangPref, savedVoice) { chosen -> 
      scope.launch { store.setTtsVoice(chosen) } 
    }

    // Whisper model download
    if (provider == "whisper") {
      OutlinedButton(
        onClick = { 
          scope.launch { WhisperUiActions.downloadTinyModel(context) } 
        }
      ) { 
        Text("Download Whisper Tiny model") 
      }
    }
    
    // Service controls
    ServiceControls(provider = provider)

    // Export functionality
    ExportButton(db = db)
  }
}

@Composable
private fun StatusHeader(
    caption: String,
    asrState: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "WristLingo\n$caption",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        
        // ASR state indicator
        AsrStateIndicator(asrState = asrState)
    }
}

@Composable
private fun AsrStateIndicator(
    asrState: String,
    modifier: Modifier = Modifier
) {
    val color = when (asrState) {
        "ready" -> Color(0xFFFBC02D)
        "listening" -> Color(0xFF2E7D32)
        "processing" -> Color(0xFF0288D1)
        "error" -> Color(0xFFC62828)
        else -> Color(0xFF9E9E9E)
    }
    
    val scale = if (asrState == "listening") {
        val transition = rememberInfiniteTransition(label = "asr-pulse")
        transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "asr-pulse-scale"
        ).value
    } else 1f
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text("ASR: ${asrState.replaceFirstChar { it.uppercase() }}")
    }
}

@Composable
private fun ExportButton(
    db: AppDatabase,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(
        CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { exportSessions(context, db, uri) }
        }
    }
    
    OutlinedButton(
        onClick = { exportLauncher.launch("wristlingo-sessions.jsonl") },
        modifier = modifier
    ) { 
        Text("Export Sessions") 
    }
}


@Preview
@Composable
private fun PreviewHome() {
  HomeScreen()
}
