package com.example.wristlingo.wear

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearBridge(private val context: Context, private val onControl: ((String) -> Unit)? = null) : MessageClient.OnMessageReceivedListener {
  private val scope = CoroutineScope(Dispatchers.IO)
  private val messageClient by lazy { Wearable.getMessageClient(context) }

  fun start() { messageClient.addListener(this) }
  fun stop() { messageClient.removeListener(this) }

  fun broadcastCaption(text: String) {
    scope.launch {
      val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
      for (n in nodes) {
        messageClient.sendMessage(n.id, PATH_CAPTION, text.toByteArray())
      }
    }
  }

  override fun onMessageReceived(event: MessageEvent) {
    when (event.path) {
      PATH_CONTROL -> {
        val cmd = String(event.data)
        onControl?.invoke(cmd)
      }
    }
  }

  companion object {
    const val PATH_CAPTION = "/caption/update"
    const val PATH_CONTROL = "/session/control"
  }
}
