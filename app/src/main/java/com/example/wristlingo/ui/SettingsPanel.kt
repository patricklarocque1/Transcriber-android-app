package com.example.wristlingo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.example.wristlingo.settings.SettingsApi
import kotlinx.coroutines.launch

/**
 * UI component for app settings (language, PII redaction, TTS, etc.)
 */
@Composable
fun SettingsPanel(
    targetLang: String,
    redact: Boolean,
    tts: Boolean,
    autoDetect: Boolean,
    ttsPitch: Float,
    ttsRate: Float,
    store: SettingsApi,
    testTagsEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var langInput by remember(targetLang) { mutableStateOf(targetLang) }
    var pitchInput by remember(ttsPitch) { mutableStateOf(ttsPitch) }
    var rateInput by remember(ttsRate) { mutableStateOf(ttsRate) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language setting
        OutlinedTextField(
            value = langInput,
            onValueChange = { langInput = it },
            label = { Text("Target lang (e.g., es)") },
            modifier = Modifier.fillMaxWidth().let { m -> if (testTagsEnabled) m.testTag("input_lang") else m }
        )
        
        OutlinedButton(
            onClick = { 
                scope.launch { store.setTargetLang(langInput) }
            },
            modifier = if (testTagsEnabled) Modifier.testTag("btn_save_lang") else Modifier
        ) { 
            Text("Save Lang") 
        }
        
        // Toggle settings
        SettingToggle(
            label = "Redact PII",
            checked = redact,
            onCheckedChange = { scope.launch { store.setRedact(it) } },
            switchTestTag = if (testTagsEnabled) "switch_redact" else null
        )
        
        SettingToggle(
            label = "TTS",
            checked = tts,
            onCheckedChange = { scope.launch { store.setTts(it) } },
            switchTestTag = if (testTagsEnabled) "switch_tts" else null
        )
        
        SettingToggle(
            label = "Auto Lang Detect",
            checked = autoDetect,
            onCheckedChange = { scope.launch { store.setAutoLangDetect(it) } },
            switchTestTag = if (testTagsEnabled) "switch_auto" else null
        )
        
        // TTS settings
        SettingSlider(
            label = "Pitch",
            value = pitchInput,
            onValueChange = { pitchInput = it },
            onSave = { scope.launch { store.setTtsPitch(pitchInput) } },
            range = 0.5f..1.5f
        )
        
        SettingSlider(
            label = "Rate",
            value = rateInput,
            onValueChange = { rateInput = it },
            onSave = { scope.launch { store.setTtsRate(rateInput) } },
            range = 0.5f..1.5f
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    switchTestTag: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(label)
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onSave: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("$label: ${String.format("%.2f", value)}")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
        OutlinedButton(onClick = onSave) {
            Text("Save $label")
        }
    }
}