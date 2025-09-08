package com.example.wristlingo.export

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.data.db.Session
import com.example.wristlingo.data.db.Utterance
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.StringWriter

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ExportJsonlTest {
  private lateinit var db: AppDatabase

  @Before
  fun setUp() {
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDatabase::class.java
    ).allowMainThreadQueries().build()
  }

  @After
  fun tearDown() { db.close() }

  @Test
  fun jsonlExport_roundTripOrderAndEscaping() = kotlinx.coroutines.test.runTest {
    val sessionId = db.sessionDao().insert(Session(startedAt = 1000L))
    db.utteranceDao().insert(Utterance(sessionId = sessionId, ts = 1100L, srcText = "Hello\nWorld", dstText = "Hola\"Mundo", lang = "es"))
    db.utteranceDao().insert(Utterance(sessionId = sessionId, ts = 1200L, srcText = null, dstText = "", lang = null))

    val writer = StringWriter()
    exportSessionsToWriter(db, writer)
    val out = writer.toString().trim()

    assertTrue(out.lines()[0].contains("\"type\":\"session\""))
    assertTrue(out.lines()[1].contains("\\n"))
    assertTrue(out.lines()[1].contains("\\\"Mundo"))
    assertTrue(out.lines()[2].contains("\"dstText\":\"\""))
  }
}

