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
class SessionDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var sessionDao: SessionDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = database.sessionDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveSession() = runTest {
        val session = Session(startedAt = System.currentTimeMillis())
        val sessionId = sessionDao.insert(session)
        
        assertTrue("Session ID should be positive", sessionId > 0)
        
        val sessions = sessionDao.all()
        assertEquals("Should have one session", 1, sessions.size)
        assertEquals("Session should have correct timestamp", session.startedAt, sessions[0].startedAt)
    }

    @Test
    fun endSession() = runTest {
        val session = Session(startedAt = System.currentTimeMillis())
        val sessionId = sessionDao.insert(session)
        
        val endTime = System.currentTimeMillis() + 1000
        sessionDao.end(sessionId, endTime)
        
        val sessions = sessionDao.all()
        assertEquals("Should have one session", 1, sessions.size)
        assertEquals("Session should have end time", endTime, sessions[0].endedAt)
    }

    @Test
    fun sessionsOrderedByStartTimeDesc() = runTest {
        val session1 = Session(startedAt = 1000L)
        val session2 = Session(startedAt = 2000L)
        val session3 = Session(startedAt = 1500L)
        
        sessionDao.insert(session1)
        sessionDao.insert(session2)
        sessionDao.insert(session3)
        
        val sessions = sessionDao.all()
        assertEquals("Should have three sessions", 3, sessions.size)
        assertEquals("First session should be most recent", 2000L, sessions[0].startedAt)
        assertEquals("Second session should be middle", 1500L, sessions[1].startedAt)
        assertEquals("Third session should be oldest", 1000L, sessions[2].startedAt)
    }

    @Test
    fun replaceSessionOnConflict() = runTest {
        val session = Session(id = 1, startedAt = 1000L)
        sessionDao.insert(session)
        
        val updatedSession = Session(id = 1, startedAt = 2000L)
        sessionDao.insert(updatedSession)
        
        val sessions = sessionDao.all()
        assertEquals("Should have one session", 1, sessions.size)
        assertEquals("Session should have updated timestamp", 2000L, sessions[0].startedAt)
    }
}