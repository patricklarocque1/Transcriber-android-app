package com.example.wristlingo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {
  private val messageClient by lazy { Wearable.getMessageClient(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    messageClient.addListener(this)
    setContent { WearScreen(onStart = { send("/session/control", "start") }, onStop = { send("/session/control", "stop") }) }
  }

  override fun onDestroy() {
    messageClient.removeListener(this)
    super.onDestroy()
  }

  private fun send(path: String, msg: String) {
    Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
      nodes.forEach { messageClient.sendMessage(it.id, path, msg.toByteArray()) }
    }
  }

  private var captionState: MutableState<String>? = null
  override fun onMessageReceived(messageEvent: MessageEvent) {
    if (messageEvent.path == "/caption/update") {
      val text = String(messageEvent.data)
      captionState?.value = text
    }
  }

  @Composable
  private fun WearScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val caption = remember { mutableStateOf("Idle") }
    captionState = caption
    Column(
      modifier = Modifier.fillMaxSize().padding(8.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(caption.value)
      Button(onClick = onStart) { Text("Start") }
      Button(onClick = onStop) { Text("Stop") }
    }
  }
}
