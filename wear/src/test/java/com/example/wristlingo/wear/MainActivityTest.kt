package com.example.wristlingo.wear

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.wearable.MessageEvent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun displaysIdleStateInitially() {
        composeTestRule.onNodeWithText("Idle").assertExists()
    }

    @Test
    fun displaysStartAndStopButtons() {
        composeTestRule.onNodeWithText("Start").assertExists()
        composeTestRule.onNodeWithText("Stop").assertExists()
    }

    @Test
    fun startButtonClickable() {
        // Test that start button is clickable without crashing
        composeTestRule.onNodeWithText("Start").performClick()
        // Should not crash and UI should still be responsive
        composeTestRule.onNodeWithText("Start").assertExists()
    }

    @Test
    fun stopButtonClickable() {
        // Test that stop button is clickable without crashing
        composeTestRule.onNodeWithText("Stop").performClick()
        // Should not crash and UI should still be responsive
        composeTestRule.onNodeWithText("Stop").assertExists()
    }

    @Test
    fun onMessageReceivedUpdatesCaptionState() {
        val activity = composeTestRule.activity
        
        // Simulate receiving a caption message
        val mockMessageEvent = mock<MessageEvent>()
        whenever(mockMessageEvent.path).thenReturn("/caption/update")
        whenever(mockMessageEvent.data).thenReturn("Hello World".toByteArray())
        
        activity.onMessageReceived(mockMessageEvent)
        
        // Caption should be updated in the UI
        composeTestRule.onNodeWithText("Hello World").assertExists()
    }

    @Test
    fun ignoresNonCaptionMessages() {
        val activity = composeTestRule.activity
        
        // Simulate receiving a non-caption message
        val mockMessageEvent = mock<MessageEvent>()
        whenever(mockMessageEvent.path).thenReturn("/other/path")
        whenever(mockMessageEvent.data).thenReturn("Other Message".toByteArray())
        
        activity.onMessageReceived(mockMessageEvent)
        
        // Caption should remain as "Idle"
        composeTestRule.onNodeWithText("Idle").assertExists()
        composeTestRule.onNodeWithText("Other Message").assertDoesNotExist()
    }
}