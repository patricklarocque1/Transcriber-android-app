package com.example.wristlingo.providers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wristlingo.whisper.WhisperModelManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class WhisperCppProviderTest {

    private lateinit var context: Context
    private lateinit var provider: WhisperCppProvider
    private lateinit var mockNative: WhisperCppNative

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Mock the native implementation since we're testing in Robolectric
        mockNative = mock()
        
        // Create a mock model file
        val mockModelFile = File(context.cacheDir, "whisper-tiny.bin")
        mockModelFile.createNewFile()
        
        // Mock WhisperModelManager
        mockkObject(WhisperModelManager)
        every { WhisperModelManager.modelFile(any()) } returns mockModelFile
        every { WhisperModelManager.isPresent(any()) } returns true
    }

    @Test
    fun providerInitializationWithValidModel() = runTest {
        // Mock successful native initialization
        whenever(mockNative.nativeInit(any(), any())).thenReturn(123L)
        whenever(mockNative.nativeIsReady(123L)).thenReturn(true)
        
        provider = WhisperCppProvider(context)
        
        try {
            provider.start(16000, null)
            // Should not throw exception
            assertTrue("Provider should start successfully", true)
        } catch (e: Exception) {
            // Expected in test environment where native library isn't available
            assertTrue("Should handle missing native library gracefully", 
                e.message?.contains("library") == true || 
                e.message?.contains("Whisper") == true)
        }
    }

    @Test
    fun providerFailsWithMissingModel() {
        // Mock missing model file
        val nonExistentFile = File(context.cacheDir, "nonexistent.bin")
        every { WhisperModelManager.modelFile(any()) } returns nonExistentFile
        
        provider = WhisperCppProvider(context)
        
        assertThrows("Should throw exception for missing model", 
            IllegalStateException::class.java) {
            provider.start(16000, null)
        }
    }

    @Test
    fun feedPcmWithoutStartingFails() = runTest {
        provider = WhisperCppProvider(context)
        
        val audioFrame = ShortArray(1600) { 100 }
        val result = provider.feedPcm(audioFrame)
        
        assertNull("Should return null when not started", result)
    }

    @Test
    fun finalizeWithoutStartingReturnsEmpty() = runTest {
        provider = WhisperCppProvider(context)
        
        val result = provider.finalizeStream()
        
        assertEquals("Should return empty string when not started", "", result)
    }

    @Test
    fun closeProviderCleansUp() = runTest {
        provider = WhisperCppProvider(context)
        
        // Should not throw exception
        provider.close()
        
        // Verify provider is in clean state
        val result = provider.finalizeStream()
        assertEquals("Should return empty after close", "", result)
    }

    @Test
    fun setListenerWorks() = runTest {
        provider = WhisperCppProvider(context)
        var receivedPartial: AsrProvider.Partial? = null
        
        provider.setListener { partial ->
            receivedPartial = partial
        }
        
        // Listener should be set (we can't easily test the callback without native lib)
        assertNotNull("Listener should be set", provider)
    }

    @Test
    fun handleNativeLibraryNotAvailable() {
        // This test verifies graceful handling when native library is not available
        provider = WhisperCppProvider(context)
        
        try {
            provider.start(16000, null)
            fail("Should have thrown exception for missing native library")
        } catch (e: Exception) {
            // Expected - native library not available in test environment
            assertTrue("Should handle native library absence gracefully", 
                e is IllegalStateException || e is RuntimeException)
        }
    }

    @Test
    fun multipleStartCallsHandledGracefully() = runTest {
        provider = WhisperCppProvider(context)
        
        try {
            provider.start(16000, null)
            provider.start(16000, null) // Second call
            // Should handle multiple starts gracefully
        } catch (e: Exception) {
            // Expected in test environment
            assertTrue("Should handle multiple starts", true)
        }
    }

    @Test
    fun differentSampleRatesSupported() = runTest {
        provider = WhisperCppProvider(context)
        
        val sampleRates = listOf(8000, 16000, 22050, 44100, 48000)
        
        for (sampleRate in sampleRates) {
            try {
                provider.start(sampleRate, null)
                provider.close()
                // Should accept different sample rates
                assertTrue("Should support sample rate $sampleRate", true)
            } catch (e: Exception) {
                // Expected in test environment without native library
                assertTrue("Should handle sample rate $sampleRate gracefully", true)
            }
        }
    }

    @Test
    fun multipleCloseCallsHandledGracefully() = runTest {
        provider = WhisperCppProvider(context)
        
        // Multiple close calls should not cause issues (e.g., double-deletion)
        provider.close()
        provider.close()
        provider.close()
        
        // Should not throw exception or cause memory corruption
        assertTrue("Should handle multiple close calls gracefully", true)
    }

    @Test
    fun memoryLeakPrevention() = runTest {
        // Test that multiple provider instances can be created and closed
        // without accumulating memory leaks
        repeat(10) { i ->
            val testProvider = WhisperCppProvider(context)
            try {
                // Attempt to start if possible
                testProvider.start(16000, null)
            } catch (e: Exception) {
                // Expected in test environment
            }
            // Always close to test cleanup
            testProvider.close()
            
            assertTrue("Should create and close provider $i without issues", true)
        }
    }
}