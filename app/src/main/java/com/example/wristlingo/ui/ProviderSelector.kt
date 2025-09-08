package com.example.wristlingo.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wristlingo.settings.SettingsApi
import kotlinx.coroutines.launch

/**
 * UI component for selecting ASR provider (System, Whisper)
 */
@Composable
fun ProviderSelector(
    currentProvider: String,
    store: SettingsApi,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Text("ASR Provider: $currentProvider")
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    scope.launch { store.setProvider("system") }
                }
            ) { 
                Text("System") 
            }
            
            OutlinedButton(
                onClick = { 
                    scope.launch {
                        store.setProvider("whisper")
                        showWhisperMessage(context)
                    }
                }
            ) { 
                Text("Whisper") 
            }
        }
    }
}

private fun showWhisperMessage(context: Context) {
    Toast.makeText(
        context, 
        "Whisper provider available - ensure model is downloaded", 
        Toast.LENGTH_SHORT
    ).show()
}