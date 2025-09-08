package com.example.wristlingo.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.data.db.Session
import com.example.wristlingo.data.Redactor
import com.example.wristlingo.providers.*
import com.example.wristlingo.service.TranslationPipeline
import com.example.wristlingo.lang.LanguageId
import com.example.wristlingo.wear.WearBridge
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EndToEndDataFlowTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var mockWearBridge: WearBridge
    private lateinit var mockTranslationProvider: TranslationProvider
    private lateinit var mockLanguageId: LanguageId

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        mockWearBridge = mock()
        mockTranslationProvider = mock()
        mockLanguageId = mock()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeTranslationPipelineFlow() = runTest {
        // Setup mock responses
        whenever(mockLanguageId.detect(any())).thenReturn("en")
        whenever(mockTranslationProvider.translate(any(), any(), any())).thenReturn("Hola mundo")
        
        val pipeline = TranslationPipeline(
            context = context,
            translationProvider = mockTranslationProvider,
            db = database,
            wearBridge = mockWearBridge,
            langId = mockLanguageId
        )
        
        val config = TranslationPipeline.Config(
            targetLang = "es",
            redact = false,
            ttsEnabled = false,
            autoDetect = true
        )
        
        // Create session
        val sessionId = database.sessionDao().insert(
            Session(startedAt = System.currentTimeMillis())
        )
        
        // Process text through pipeline
        pipeline.processText("Hello world", true, sessionId, config)
        
        // Verify database persistence
        val utterances = database.utteranceDao().bySession(sessionId)
        assertEquals("Should have one utterance", 1, utterances.size)
        assertEquals("Should have source text", "Hello world", utterances[0].srcText)
        assertEquals("Should have translated text", "Hola mundo", utterances[0].dstText)
        assertEquals("Should have target language", "es", utterances[0].lang)
        
        // Verify wear bridge was called
        verify(mockWearBridge).broadcastCaption("Hola mundo")
        
        // Verify translation provider was called
        verify(mockTranslationProvider).translate("Hello world", "en", "es")
    }

    @Test
    fun piiRedactionInPipeline() = runTest {
        whenever(mockTranslationProvider.translate(any(), any(), any())).thenAnswer { 
            it.getArgument<String>(0) // Return input as-is for test
        }
        
        val pipeline = TranslationPipeline(
            context = context,
            translationProvider = mockTranslationProvider,
            db = database,
            wearBridge = mockWearBridge,
            langId = mockLanguageId
        )
        
        val config = TranslationPipeline.Config(
            targetLang = "es",
            redact = true,
            ttsEnabled = false,
            autoDetect = false
        )
        
        val sessionId = database.sessionDao().insert(
            Session(startedAt = System.currentTimeMillis())
        )
        
        // Process text with PII
        pipeline.processText("Contact me at user@example.com", true, sessionId, config)
        
        // Verify PII was redacted in database
        val utterances = database.utteranceDao().bySession(sessionId)
        assertEquals("Should have one utterance", 1, utterances.size)
        assertEquals("Should have redacted PII", "Contact me at [email]", utterances[0].srcText)
        
        // Verify translation provider received redacted text
        verify(mockTranslationProvider).translate("Contact me at [email]", null, "es")
    }

    @Test
    fun asrProviderToTranslationFlow() = runTest {
        val fakeAsrProvider = FakeAsrProvider()
        var finalTranslation: String? = null
        val latch = CountDownLatch(1)
        
        // Setup translation mock
        whenever(mockTranslationProvider.translate(any(), any(), any())).thenAnswer {
            val input = it.getArgument<String>(0)
            "$input translated".also { result ->
                finalTranslation = result
                latch.countDown()
            }
        }
        
        val pipeline = TranslationPipeline(
            context = context,
            translationProvider = mockTranslationProvider,
            db = database,
            wearBridge = mockWearBridge,
            langId = mockLanguageId
        )
        
        val config = TranslationPipeline.Config(
            targetLang = "fr",
            redact = false,
            ttsEnabled = false,
            autoDetect = false
        )
        
        val sessionId = database.sessionDao().insert(
            Session(startedAt = System.currentTimeMillis())
        )
        
        // Set up ASR provider listener
        fakeAsrProvider.setListener { partial ->
            if (partial.isFinal) {
                kotlinx.coroutines.runBlocking {
                    pipeline.processText(partial.text, true, sessionId, config)
                }
            }
        }
        
        // Simulate ASR processing
        fakeAsrProvider.start(16000, null)
        val audioFrame = ShortArray(1600) { 100 }
        fakeAsrProvider.feedPcm(audioFrame)
        val finalText = fakeAsrProvider.finalizeStream()
        
        // Process final result
        pipeline.processText(finalText, true, sessionId, config)
        
        // Wait for async processing
        assertTrue("Should complete processing", latch.await(5, TimeUnit.SECONDS))
        
        // Verify end-to-end flow
        assertNotNull("Should have final translation", finalTranslation)
        assertTrue("Should contain translated suffix", finalTranslation!!.contains("translated"))
        
        fakeAsrProvider.close()
    }

    @Test
    fun phoneToWearMessageFlow() = runTest {
        val captionLatch = CountDownLatch(1)
        var broadcastedCaption: String? = null
        
        // Mock wear bridge to capture broadcasts
        doAnswer { invocation ->
            broadcastedCaption = invocation.getArgument<String>(0)
            captionLatch.countDown()
            null
        }.whenever(mockWearBridge).broadcastCaption(any())
        
        whenever(mockTranslationProvider.translate(any(), any(), any())).thenReturn("Bonjour")
        
        val pipeline = TranslationPipeline(
            context = context,
            translationProvider = mockTranslationProvider,
            db = database,
            wearBridge = mockWearBridge,
            langId = mockLanguageId
        )
        
        val config = TranslationPipeline.Config(
            targetLang = "fr",
            redact = false,
            ttsEnabled = false,
            autoDetect = false
        )
        
        val sessionId = database.sessionDao().insert(
            Session(startedAt = System.currentTimeMillis())
        )
        
        // Process text
        pipeline.processText("Hello", true, sessionId, config)
        
        // Wait for wear broadcast
        assertTrue("Should broadcast to wear", captionLatch.await(5, TimeUnit.SECONDS))
        assertEquals("Should broadcast translated text", "Bonjour", broadcastedCaption)
    }

    @Test
    fun multipleUtterancesInSession() = runTest {
        whenever(mockTranslationProvider.translate(any(), any(), any())).thenAnswer {
            "Translated: ${it.getArgument<String>(0)}"
        }
        
        val pipeline = TranslationPipeline(
            context = context,
            translationProvider = mockTranslationProvider,
            db = database,
            wearBridge = mockWearBridge,
            langId = mockLanguageId
        )
        
        val config = TranslationPipeline.Config(
            targetLang = "de",
            redact = false,
            ttsEnabled = false,
            autoDetect = false
        )
        
        val sessionId = database.sessionDao().insert(
            Session(startedAt = System.currentTimeMillis())
        )
        
        // Process multiple utterances
        val utterances = listOf("First", "Second", "Third")
        for (utterance in utterances) {
            pipeline.processText(utterance, true, sessionId, config)
        }
        
        // Verify all utterances were stored
        val storedUtterances = database.utteranceDao().bySession(sessionId)
        assertEquals("Should have all utterances", 3, storedUtterances.size)
        
        // Verify order and content
        for (i in utterances.indices) {
            assertEquals("Should have correct source text", utterances[i], storedUtterances[i].srcText)
            assertEquals("Should have translated text", "Translated: ${utterances[i]}", storedUtterances[i].dstText)
        }
        
        // Verify wear bridge was called for each
        verify(mockWearBridge, times(3)).broadcastCaption(any())
    }

    @Test
    fun errorHandlingInPipeline() = runTest {
        // Setup translation provider to throw exception
        whenever(mockTranslationProvider.translate(any(), any(), any())).thenThrow(RuntimeException("Translation failed"))
        
        val pipeline = TranslationPipeline(
            context = context,
            translationProvider = mockTranslationProvider,
            db = database,
            wearBridge = mockWearBridge,
            langId = mockLanguageId
        )
        
        val config = TranslationPipeline.Config(
            targetLang = "es",
            redact = false,
            ttsEnabled = false,
            autoDetect = false
        )
        
        val sessionId = database.sessionDao().insert(
            Session(startedAt = System.currentTimeMillis())
        )
        
        // Process text - should handle error gracefully
        pipeline.processText("Hello", true, sessionId, config)
        
        // Verify fallback behavior - original text should be used
        val utterances = database.utteranceDao().bySession(sessionId)
        assertEquals("Should have one utterance", 1, utterances.size)
        assertEquals("Should use original text as fallback", "Hello", utterances[0].dstText)
        
        // Should still broadcast something to wear
        verify(mockWearBridge).broadcastCaption(any())
    }
}