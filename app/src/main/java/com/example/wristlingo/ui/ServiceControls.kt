package com.example.wristlingo.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.wristlingo.TranslatorService

/**
 * UI component for service start/stop controls with permission handling
 */
@Composable
fun ServiceControls(
    provider: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
        if (granted) {
            startService(context, TranslatorService.ACTION_START)
        } else {
            Toast.makeText(context, "Mic permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                handleStartService(context, provider, notifPermissionLauncher, micPermissionLauncher)
            }
        ) {
            Text("Start")
        }

        Button(
            onClick = {
                Toast.makeText(context, "Stopping…", Toast.LENGTH_SHORT).show()
                startService(context, TranslatorService.ACTION_STOP)
            }
        ) {
            Text("Stop")
        }
    }
}

private fun handleStartService(
    context: Context,
    provider: String,
    notifPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    micPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    // Check notification permission first (API 33+)
    if (Build.VERSION.SDK_INT >= 33) {
        val notifGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!notifGranted) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
    }

    // Check microphone permission for system provider
    if (provider == "system") {
        val micGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!micGranted) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
    }

    Toast.makeText(context, "Starting…", Toast.LENGTH_SHORT).show()
    startService(context, TranslatorService.ACTION_START)
}

private fun startService(context: Context, action: String) {
    val intent = Intent(context, TranslatorService::class.java).setAction(action)
    val isStart = action == TranslatorService.ACTION_START
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isStart) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }
}