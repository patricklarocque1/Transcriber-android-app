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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.core.content.ContextCompat
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest

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

  // Observe caption updates from the service via AppBus
  LaunchedEffect(Unit) {
    AppBus.captions.collectLatest { caption = it }
  }

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

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "WristLingo (test mode)\n$caption",
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleMedium
    )
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
      Toast.makeText(context, "Starting…", Toast.LENGTH_SHORT).show()
      startService(context, TranslatorService.ACTION_START)
    }) { Text("Start") }

    Button(onClick = {
      Toast.makeText(context, "Stopping…", Toast.LENGTH_SHORT).show()
      startService(context, TranslatorService.ACTION_STOP)
    }) {
      Text("Stop")
    }
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
