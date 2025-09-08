package com.example.wristlingo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.speech.SpeechRecognizer
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.data.db.Session
import com.example.wristlingo.providers.*
import com.example.wristlingo.service.AudioProcessor
import com.example.wristlingo.service.ServiceConfiguration
import com.example.wristlingo.service.TranslationPipeline
import com.example.wristlingo.settings.SettingsStore
import com.example.wristlingo.wear.WearBridge
import com.example.wristlingo.lang.LanguageId
import com.example.wristlingo.whisper.WhisperModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TranslatorService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var runningJob: Job? = null
  private var isForeground = false
  
  // Core components
  private lateinit var settings: SettingsStore
  private lateinit var db: AppDatabase
  private lateinit var wear: WearBridge
  private lateinit var langId: LanguageId
  
  // Service components
  private lateinit var serviceConfig: ServiceConfiguration
  private lateinit var translationPipeline: TranslationPipeline
  private lateinit var audioProcessor: AudioProcessor
  
  private var currentSessionId: Long? = null
  // Test seams: set in tests to avoid real audio/translation side effects
  internal var translationProviderFactory: (() -> com.example.wristlingo.providers.TranslationProvider)? = null
  internal var processAudioHook: (suspend (com.example.wristlingo.providers.AsrProvider, kotlin.coroutines.CoroutineContext, suspend (String) -> Unit) -> Unit)? = null

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()
    
    // Initialize core components
    settings = SettingsStore(this)
    db = AppDatabase.get(this)
    wear = WearBridge(this) { cmd ->
      when (cmd.lowercase()) {
        "start" -> onStartCommand(Intent(ACTION_START), 0, 0)
        "stop" -> onStartCommand(Intent(ACTION_STOP), 0, 0)
      }
    }.also { it.start() }
    langId = LanguageId(this)
    
    // Initialize service components
    serviceConfig = ServiceConfiguration(this)
    audioProcessor = AudioProcessor()
  }

  override fun onDestroy() {
    runningJob?.cancel()
    wear.stop()
    
    // Cleanup service components
    if (::translationPipeline.isInitialized) {
      translationPipeline.shutdown()
    }
    
    stopForeground(STOP_FOREGROUND_REMOVE)
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
            Log.w(TAG, "startForeground not allowed, stopping: $t")
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
    return START_NOT_STICKY
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    stopSimulation()
    super.onTaskRemoved(rootIntent)
  }

  private fun startWork() {
    if (runningJob?.isActive == true) return
    runningJob = serviceScope.launch {
      try {
        // Load configuration
        val config = serviceConfig.loadConfig(settings)
        
        // Validate configuration
        val issues = serviceConfig.validateConfig(config)
        if (issues.isNotEmpty()) {
          AppBus.captions.tryEmit("Configuration issues: ${issues.joinToString(", ")}")
          delay(2000)
          return@launch
        }
        
        // Create providers
        val translationProvider = translationProviderFactory?.invoke() ?: serviceConfig.createTranslationProvider()
        val asrProvider = serviceConfig.createAsrProvider(config.providerId)
        
        // Initialize translation pipeline
        translationPipeline = TranslationPipeline(
          context = this@TranslatorService,
          translationProvider = translationProvider,
          db = db,
          wearBridge = wear,
          langId = langId
        )
        
        val pipelineConfig = TranslationPipeline.Config(
          targetLang = config.targetLang,
          redact = config.redact,
          ttsEnabled = config.ttsEnabled,
          autoDetect = config.autoDetect,
          ttsPitch = config.ttsPitch,
          ttsRate = config.ttsRate,
          ttsVoice = config.ttsVoice
        )
        
        translationPipeline.initializeTts(pipelineConfig)
        
        // Start session
        currentSessionId = db.sessionDao().insert(Session(startedAt = System.currentTimeMillis()))
        
        // Handle different provider types
        when {
          config.providerId == "whisper" && !WhisperModelManager.isPresent(this@TranslatorService) -> {
            AppBus.captions.tryEmit("Whisper model missing. Download in app first.")
            delay(1500)
          }
          
          config.providerId == "system" && SpeechRecognizer.isRecognitionAvailable(this@TranslatorService) -> {
            handleSystemSpeechRecognizer(asrProvider as SystemSpeechRecognizerProvider, pipelineConfig)
          }
          
          else -> {
            handleAudioRecording(asrProvider, pipelineConfig)
          }
        }
        
      } catch (e: Exception) {
        Log.e(TAG, "Error in startWork", e)
        AppBus.captions.tryEmit("Service error: ${e.message}")
      }
    }
  }
  
  private suspend fun handleSystemSpeechRecognizer(
    asr: SystemSpeechRecognizerProvider,
    pipelineConfig: TranslationPipeline.Config
  ) {
    asr.setListener { part -> 
      serviceScope.launch { 
        translationPipeline.processText(part.text, part.isFinal, currentSessionId!!, pipelineConfig)
      }
    }
    
    try {
      asr.start(16_000, null)
      while (serviceScope.coroutineContext.isActive) delay(500)
    } finally {
      asr.close()
    }
  }
  
  private suspend fun handleAudioRecording(
    asr: AsrProvider,
    pipelineConfig: TranslationPipeline.Config
  ) {
    asr.setListener { part ->
      serviceScope.launch {
        translationPipeline.processText(part.text, part.isFinal, currentSessionId!!, pipelineConfig)
      }
    }
    
    val hook = processAudioHook
    if (hook != null) {
      hook(asr, serviceScope.coroutineContext) { finalText ->
        translationPipeline.processText(finalText, true, currentSessionId!!, pipelineConfig)
      }
    } else {
      audioProcessor.processAudio(asr, serviceScope.coroutineContext) { finalText ->
        translationPipeline.processText(finalText, true, currentSessionId!!, pipelineConfig)
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
    AppBus.asrState.value = "idle"
  }

  companion object {
    private const val CHANNEL_ID = "wristlingo.capture"
    private const val NOTIF_ID = 1
    private const val TAG = "TranslatorService"
    const val ACTION_START = "com.example.wristlingo.action.START"
    const val ACTION_STOP = "com.example.wristlingo.action.STOP"
  }
}
