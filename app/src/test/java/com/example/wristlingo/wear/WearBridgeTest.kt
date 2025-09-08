package com.example.wristlingo.wear

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class WearBridgeTest {
  @Test
  fun broadcastsCaptionToAllNodes() = runTest {
    val context: Context = ApplicationProvider.getApplicationContext()

    val msgClient = mockk<MessageClient>(relaxed = true)
    val nodeClient = mockk<NodeClient>()
    val node: Node = mockk { every { id } returns "node-1" }
    val task: Task<List<Node>> = Tasks.forResult(listOf(node))
    every { nodeClient.connectedNodes } returns task

    val bridge = WearBridge(context, onControl = null, injectedMessageClient = msgClient, injectedNodeClient = nodeClient)
    bridge.broadcastCaption("Hello")

    // Allow coroutine to run
    Thread.sleep(50)

    verify { msgClient.sendMessage("node-1", WearBridge.PATH_CAPTION, any()) }
  }
}

