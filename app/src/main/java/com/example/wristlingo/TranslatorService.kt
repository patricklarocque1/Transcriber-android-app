package com.example.wristlingo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class TranslatorService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()
    startForeground(1, buildNotification())
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val mgr = getSystemService(NotificationManager::class.java)
      val id = CHANNEL_ID
      if (mgr.getNotificationChannel(id) == null) {
        mgr.createNotificationChannel(
          NotificationChannel(id, "WristLingo Capture", NotificationManager.IMPORTANCE_LOW)
        )
      }
    }
  }

  private fun buildNotification(): Notification {
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      android.app.Notification.Builder(this, CHANNEL_ID)
    } else {
      @Suppress("DEPRECATION")
      android.app.Notification.Builder(this)
    }
    return builder
      .setContentTitle("WristLingo running")
      .setContentText("Capturing audio for translation")
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .build()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // TODO: Wire Data Layer + providers per docs/providers.md
    return START_STICKY
  }

  companion object { private const val CHANNEL_ID = "wristlingo.capture" }
}

