package com.example.wristlingo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class Session(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "started_at") val startedAt: Long,
  @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
)

@Entity(tableName = "utterance")
data class Utterance(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "session_id") val sessionId: Long,
  @ColumnInfo(name = "ts") val ts: Long,
  @ColumnInfo(name = "src_text") val srcText: String?,
  @ColumnInfo(name = "dst_text") val dstText: String?,
  @ColumnInfo(name = "lang") val lang: String?,
)

