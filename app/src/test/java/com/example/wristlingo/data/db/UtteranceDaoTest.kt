package com.example.wristlingo.data.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class UtteranceDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var utteranceDao: UtteranceDao
    private lateinit var sessionDao: SessionDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        utteranceDao = database.utteranceDao()
        sessionDao = database.sessionDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveUtterance() = runTest {
        val sessionId = sessionDao.insert(Session(startedAt = System.currentTimeMillis()))
        
        val utterance = Utterance(
            sessionId = sessionId,
            ts = System.currentTimeMillis(),
            srcText = "Hello",
            dstText = "Hola",
            lang = "es"
        )
        
        val utteranceId = utteranceDao.insert(utterance)
        assertTrue("Utterance ID should be positive", utteranceId > 0)
        
        val utterances = utteranceDao.bySession(sessionId)
        assertEquals("Should have one utterance", 1, utterances.size)
        assertEquals("Should have correct source text", "Hello", utterances[0].srcText)
        assertEquals("Should have correct destination text", "Hola", utterances[0].dstText)
        assertEquals("Should have correct language", "es", utterances[0].lang)
    }

    @Test
    fun utterancesOrderedByTimestampAsc() = runTest {
        val sessionId = sessionDao.insert(Session(startedAt = System.currentTimeMillis()))
        
        val utterance1 = Utterance(sessionId = sessionId, ts = 1000L, srcText = "First", dstText = "Primero", lang = "es")
        val utterance2 = Utterance(sessionId = sessionId, ts = 3000L, srcText = "Third", dstText = "Tercero", lang = "es")
        val utterance3 = Utterance(sessionId = sessionId, ts = 2000L, srcText = "Second", dstText = "Segundo", lang = "es")
        
        utteranceDao.insert(utterance1)
        utteranceDao.insert(utterance2)
        utteranceDao.insert(utterance3)
        
        val utterances = utteranceDao.bySession(sessionId)
        assertEquals("Should have three utterances", 3, utterances.size)
        assertEquals("First utterance should be earliest", "First", utterances[0].srcText)
        assertEquals("Second utterance should be middle", "Second", utterances[1].srcText)
        assertEquals("Third utterance should be latest", "Third", utterances[2].srcText)
    }

    @Test
    fun utterancesBySessionFiltered() = runTest {
        val sessionId1 = sessionDao.insert(Session(startedAt = System.currentTimeMillis()))
        val sessionId2 = sessionDao.insert(Session(startedAt = System.currentTimeMillis()))
        
        utteranceDao.insert(Utterance(sessionId = sessionId1, ts = 1000L, srcText = "Session 1", dstText = "Sesión 1", lang = "es"))
        utteranceDao.insert(Utterance(sessionId = sessionId2, ts = 2000L, srcText = "Session 2", dstText = "Sesión 2", lang = "es"))
        utteranceDao.insert(Utterance(sessionId = sessionId1, ts = 3000L, srcText = "Session 1 Again", dstText = "Sesión 1 De Nuevo", lang = "es"))
        
        val utterancesSession1 = utteranceDao.bySession(sessionId1)
        val utterancesSession2 = utteranceDao.bySession(sessionId2)
        
        assertEquals("Session 1 should have 2 utterances", 2, utterancesSession1.size)
        assertEquals("Session 2 should have 1 utterance", 1, utterancesSession2.size)
        assertEquals("Session 1 first utterance", "Session 1", utterancesSession1[0].srcText)
        assertEquals("Session 2 utterance", "Session 2", utterancesSession2[0].srcText)
    }

    @Test
    fun handleNullValues() = runTest {
        val sessionId = sessionDao.insert(Session(startedAt = System.currentTimeMillis()))
        
        val utterance = Utterance(
            sessionId = sessionId,
            ts = System.currentTimeMillis(),
            srcText = null,
            dstText = null,
            lang = null
        )
        
        val utteranceId = utteranceDao.insert(utterance)
        assertTrue("Should insert utterance with null values", utteranceId > 0)
        
        val utterances = utteranceDao.bySession(sessionId)
        assertEquals("Should have one utterance", 1, utterances.size)
        assertNull("Source text should be null", utterances[0].srcText)
        assertNull("Destination text should be null", utterances[0].dstText)
        assertNull("Language should be null", utterances[0].lang)
    }
}