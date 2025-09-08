package com.example.wristlingo.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.data.db.Session
import com.example.wristlingo.lang.LanguageId
import com.example.wristlingo.providers.TranslationProvider
import com.example.wristlingo.wear.WearBridge
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class TranslationPipelineTest {
  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var translationProvider: TranslationProvider
  private lateinit var wearBridge: WearBridge
  private lateinit var langId: LanguageId

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    translationProvider = mockk(relaxed = true)
    wearBridge = mockk(relaxed = true)
    langId = mockk(relaxed = true)
  }

  @After
  fun tearDown() { db.close() }

  @Test
  fun detectsLanguageWhenEnabledAndFinal() = runTest {
    coEvery { langId.detect(any()) } returns "en"
    coEvery { translationProvider.translate(any(), any(), any()) } returns "Hola"

    val pipeline = TranslationPipeline(context, translationProvider, db, wearBridge, langId)
    val sessionId = db.sessionDao().insert(Session(startedAt = 1L))
    val cfg = TranslationPipeline.Config(targetLang = "es", redact = false, ttsEnabled = false, autoDetect = true)

    pipeline.processText("Hello", isFinal = true, sessionId = sessionId, config = cfg)

    val utts = db.utteranceDao().bySession(sessionId)
    assertEquals(1, utts.size)
    assertEquals("Hola", utts[0].dstText)
    coVerify { translationProvider.translate("Hello", "en", "es") }
  }

  @Test
  fun bypassesDetectionWhenNotFinal() = runTest {
    coEvery { translationProvider.translate(any(), any(), any()) } returns "Hallo"

    val pipeline = TranslationPipeline(context, translationProvider, db, wearBridge, langId)
    val sessionId = db.sessionDao().insert(Session(startedAt = 1L))
    val cfg = TranslationPipeline.Config(targetLang = "de", redact = false, ttsEnabled = false, autoDetect = true)

    pipeline.processText("Hi there", isFinal = false, sessionId = sessionId, config = cfg)

    // Should not persist when not final
    val utts = db.utteranceDao().bySession(sessionId)
    assertEquals(0, utts.size)
    coVerify { translationProvider.translate("Hi there", null, "de") }
  }

  @Test
  fun redactsWhenEnabled() = runTest {
    coEvery { translationProvider.translate(any(), any(), any()) } answers { firstArg() }

    val pipeline = TranslationPipeline(context, translationProvider, db, wearBridge, langId)
    val sessionId = db.sessionDao().insert(Session(startedAt = 1L))
    val cfg = TranslationPipeline.Config(targetLang = "es", redact = true, ttsEnabled = false, autoDetect = false)

    pipeline.processText("Call +1 415 555 1212", isFinal = true, sessionId = sessionId, config = cfg)
    val utts = db.utteranceDao().bySession(sessionId)
    assertEquals("[phone]" in (utts[0].srcText ?: ""), true)
  }
}

