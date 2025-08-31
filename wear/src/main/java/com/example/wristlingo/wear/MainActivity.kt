package com.example.wristlingo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { WearGreeting() }
  }
}

@Composable
fun WearGreeting() { Text("Hello WristLingo (Wear)") }

@Preview
@Composable
fun WearGreetingPreview() { WearGreeting() }
