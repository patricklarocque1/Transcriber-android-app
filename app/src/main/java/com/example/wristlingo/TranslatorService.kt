package com.example.wristlingo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.speech.SpeechRecognizer
import com.example.wristlingo.data.Redactor
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.data.db.Session
import com.example.wristlingo.data.db.Utterance
import com.example.wristlingo.providers.*
import com.example.wristlingo.settings.Keys
import com.example.wristlingo.settings.SettingsStore
import com.example.wristlingo.wear.WearBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

class TranslatorService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var runningJob: Job? = null
  private var isForeground = false
  private lateinit var settings: SettingsStore
  private lateinit var db: AppDatabase
  private lateinit var wear: WearBridge
  private var tts: android.speech.tts.TextToSpeech? = null
  private var currentSessionId: Long? = null

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()
    settings = SettingsStore(this)
    db = AppDatabase.get(this)
    wear = WearBridge(this) { cmd ->
      when (cmd.lowercase()) {
        "start" -> onStartCommand(Intent(ACTION_START), 0, 0)
        "stop" -> onStartCommand(Intent(ACTION_STOP), 0, 0)
      }
    }.also { it.start() }
    tts = android.speech.tts.TextToSpeech(this) { }
  }

  override fun onDestroy() {
    runningJob?.cancel()
    wear.stop()
    tts?.shutdown()
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
      .setContentText("Transcribing and translating…")
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
        startWork()
      }
      ACTION_STOP -> {
        stopSimulation()
        return START_NOT_STICKY
      }
    }
    return START_STICKY
  }

  private fun startWork() {
    if (runningJob?.isActive == true) return
    runningJob = serviceScope.launch {
      val prefs = settings.data.firstOrNull()
      val providerId = prefs?.get(Keys.provider) ?: "fake"
      val targetLang = prefs?.get(Keys.targetLang) ?: "es"
      val redact = prefs?.get(Keys.redact) ?: false
      val ttsEnabled = prefs?.get(Keys.tts) ?: false

      val translator: TranslationProvider = try { MlKitTranslationProvider(this@TranslatorService) } catch (_: Throwable) { FakeTranslationProvider() }

      currentSessionId = db.sessionDao().insert(Session(startedAt = System.currentTimeMillis()))

      suspend fun handleText(text: String, isFinal: Boolean) {
        val clean = if (redact) Redactor.redact(text) else text
        val translated = try { translator.translate(clean, source = null, target = targetLang) } catch (_: Throwable) { clean }
        AppBus.captions.tryEmit(translated)
        wear.broadcastCaption(translated)
        db.utteranceDao().insert(
          Utterance(sessionId = currentSessionId!!, ts = System.currentTimeMillis(), srcText = clean, dstText = translated, lang = targetLang)
        )
        if (ttsEnabled) tts?.speak(translated, android.speech.tts.TextToSpeech.QUEUE_ADD, null, "utt-${System.currentTimeMillis()}")
      }

      if (providerId == "system" && SpeechRecognizer.isRecognitionAvailable(this@TranslatorService)) {
        val asr = SystemSpeechRecognizerProvider(this@TranslatorService).also { p ->
          p.setListener { part -> serviceScope.launch { handleText(part.text, part.isFinal) } }
        }
        asr.start(16_000, null)
        while (isActive) delay(500)
      } else {
        val asr = FakeAsrProvider().also { p -> p.setListener { part -> serviceScope.launch { handleText(part.text, part.isFinal) } } }
        val sampleRate = 16_000
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = AudioRecord.Builder()
          .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
          .setAudioFormat(
            AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).setSampleRate(sampleRate).build()
          ).setBufferSizeInBytes(minBuf * 2)
          .build()
        try {
          asr.start(sampleRate, null)
          record.startRecording()
          val buf = ShortArray(1600)
          while (isActive) {
            val read = record.read(buf, 0, buf.size)
            if (read > 0) asr.feedPcm(buf.copyOf(read))
          }
        } finally {
          record.stop(); record.release(); asr.close()
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
