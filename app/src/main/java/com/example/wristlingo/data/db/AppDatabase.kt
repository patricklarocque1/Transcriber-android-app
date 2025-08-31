package com.example.wristlingo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Session::class, Utterance::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
  abstract fun sessionDao(): SessionDao
  abstract fun utteranceDao(): UtteranceDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null
    fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
      INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "wristlingo.db").build().also { INSTANCE = it }
    }
  }
}

