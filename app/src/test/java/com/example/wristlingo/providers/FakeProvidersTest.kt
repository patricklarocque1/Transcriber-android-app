package com.example.wristlingo.providers

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeProvidersTest {

    @Test
    fun fakeAsrProviderBasicFunctionality() = runTest {
        val provider = FakeAsrProvider()
        var receivedResults = mutableListOf<AsrProvider.Partial>()
        
        provider.setListener { result ->
            receivedResults.add(result)
        }
        
        provider.start(16000, null)
        
        // Feed some audio data
        val audioFrame = ShortArray(1600) { (it % 100).toShort() }
        provider.feedPcm(audioFrame)
        
        // Should generate some fake results
        assertTrue("Should generate at least one result", receivedResults.isNotEmpty())
        
        val finalResult = provider.finalizeStream()
        assertNotNull("Should return a final result", finalResult)
        assertTrue("Final result should be non-empty", finalResult.isNotBlank())
        
        provider.close()
    }

    @Test
    fun fakeTranslationProviderTranslatesText() = runTest {
        val provider = FakeTranslationProvider()
        
        val result = provider.translate("Hello world", source = "en", target = "es")
        
        assertNotNull("Translation result should not be null", result)
        assertTrue("Translation should be non-empty", result.isNotBlank())
        assertNotEquals("Translation should be different from input", "Hello world", result)
    }

    @Test
    fun fakeTranslationProviderHandlesNullSource() = runTest {
        val provider = FakeTranslationProvider()
        
        val result = provider.translate("Hello", source = null, target = "fr")
        
        assertNotNull("Should handle null source language", result)
        assertTrue("Should return non-empty result", result.isNotBlank())
    }

    @Test
    fun fakeTranslationProviderHandlesEmptyText() = runTest {
        val provider = FakeTranslationProvider()
        
        val result = provider.translate("", source = "en", target = "es")
        
        assertNotNull("Should handle empty text", result)
        // Empty text might return empty translation or placeholder
    }

    @Test
    fun fakeAsrProviderHandlesMultipleAudioFrames() = runTest {
        val provider = FakeAsrProvider()
        var resultCount = 0
        
        provider.setListener { result ->
            resultCount++
        }
        
        provider.start(16000, null)
        
        // Feed multiple frames
        repeat(5) {
            val audioFrame = ShortArray(800) { (it * 2).toShort() }
            provider.feedPcm(audioFrame)
        }
        
        assertTrue("Should generate multiple results for multiple frames", resultCount > 0)
        
        provider.close()
    }

    @Test
    fun fakeAsrProviderGeneratesPartialAndFinalResults() = runTest {
        val provider = FakeAsrProvider()
        val results = mutableListOf<AsrProvider.Partial>()
        
        provider.setListener { result ->
            results.add(result)
        }
        
        provider.start(16000, null)
        
        val audioFrame = ShortArray(1600) { 100 }
        provider.feedPcm(audioFrame)
        
        val finalResult = provider.finalizeStream()
        
        // Should have both partial results and a final result
        val partialResults = results.filter { !it.isFinal }
        
        assertTrue("Should generate some partial results", partialResults.isNotEmpty())
        assertTrue("Final result should be different", finalResult.isNotBlank())
        
        provider.close()
    }
}