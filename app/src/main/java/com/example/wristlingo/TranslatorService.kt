package com.example.wristlingo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.wristlingo.providers.FakeAsrProvider
import com.example.wristlingo.providers.FakeTranslationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TranslatorService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var runningJob: Job? = null
  private var isForeground = false

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()
  }

  override fun onDestroy() {
    runningJob?.cancel()
    super.onDestroy()
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
      .setContentText("Simulating captions for testing")
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .build()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START, null -> {
        // Ensure we enter foreground promptly after startForegroundService
        if (!isForeground) {
          ensureNotificationChannel()
          try {
            startForeground(NOTIF_ID, buildNotification())
            isForeground = true
            Log.i(TAG, "Entered foreground (dataSync)")
            AppBus.captions.tryEmit("Service started [test mode]")
          } catch (t: Throwable) {
            Log.w(TAG, "startForeground not allowed, stopping: ${'$'}t")
            AppBus.captions.tryEmit("Unable to start service (FGS not allowed)")
            // Could not become foreground now; stop to avoid FGS timeout crash
            stopSelf(startId)
            return START_NOT_STICKY
          }
        }
        startSimulation()
      }
      ACTION_STOP -> {
        stopSimulation()
        return START_NOT_STICKY
      }
    }
    return START_STICKY
  }

  private fun startSimulation() {
    if (runningJob?.isActive == true) return
    runningJob = serviceScope.launch {
      val asr = FakeAsrProvider()
      val tr = FakeTranslationProvider()
      asr.start(sampleRate = 16_000, languageHint = null)
      tr.ensureModel(source = null, target = "es")

      val frame = ShortArray(1600) // ~100ms of 16kHz mono
      while (isActive) {
        delay(150)
        val partial = asr.feedPcm(frame)
        if (partial != null) {
          val text = if (partial.isFinal) asr.finalizeStream() else partial.text
          val translated = tr.translate(text, source = null, target = "es")
          AppBus.captions.tryEmit(translated)
          if (partial.isFinal) {
            // Brief pause then restart a new stream
            delay(400)
            asr.start(sampleRate = 16_000)
          }
        }
      }
    }
  }

  private fun stopSimulation() {
    runningJob?.cancel()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    isForeground = false
    Log.i(TAG, "Service stopped")
    AppBus.captions.tryEmit("Service stopped")
  }

  companion object {
    private const val CHANNEL_ID = "wristlingo.capture"
    private const val NOTIF_ID = 1
    private const val TAG = "TranslatorService"
    const val ACTION_START = "com.example.wristlingo.action.START"
    const val ACTION_STOP = "com.example.wristlingo.action.STOP"
  }
}
