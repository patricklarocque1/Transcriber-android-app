package com.example.wristlingo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: Session): Long

  @Query("UPDATE session SET ended_at = :end WHERE id = :id")
  suspend fun end(id: Long, end: Long)
}

@Dao
interface UtteranceDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(utt: Utterance): Long

  @Query("SELECT * FROM utterance WHERE session_id = :session ORDER BY ts ASC")
  suspend fun bySession(session: Long): List<Utterance>
}

