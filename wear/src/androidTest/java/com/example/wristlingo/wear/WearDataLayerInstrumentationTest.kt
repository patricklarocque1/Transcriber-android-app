package com.example.wristlingo.wear

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearDataLayerInstrumentationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        // Context of the app under test
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.wristlingo.wear.offline", appContext.packageName)
    }

    @Test
    fun wearScreenDisplaysCorrectly() {
        // Test that the Wear OS UI displays correctly on device
        composeTestRule.onNodeWithText("Idle").assertExists()
        composeTestRule.onNodeWithText("Start").assertExists()
        composeTestRule.onNodeWithText("Stop").assertExists()
    }

    @Test
    fun messageClientInitialization() {
        val activity = composeTestRule.activity
        val messageClient = Wearable.getMessageClient(activity)
        
        // Verify message client is properly initialized
        assert(messageClient != null)
    }

    @Test
    fun captionUpdateIntegration() {
        val activity = composeTestRule.activity
        
        // Create a mock message event for caption update
        val testCaption = "Integration Test Caption"
        val mockEvent = object : MessageEvent {
            override fun getPath(): String = "/caption/update"
            override fun getData(): ByteArray = testCaption.toByteArray()
            override fun getSourceNodeId(): String = "test_node"
            override fun getRequestId(): Int = 1
        }
        
        // Trigger the message received event
        activity.onMessageReceived(mockEvent)
        
        // Verify the UI updates
        composeTestRule.onNodeWithText(testCaption).assertExists()
    }
}