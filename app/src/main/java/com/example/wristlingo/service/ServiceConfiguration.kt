package com.example.wristlingo.service

import android.content.Context
import com.example.wristlingo.providers.*
import com.example.wristlingo.settings.Keys
import com.example.wristlingo.settings.SettingsStore
import com.example.wristlingo.whisper.WhisperModelManager
import kotlinx.coroutines.flow.firstOrNull

/**
 * Handles service configuration and provider creation based on user preferences.
 */
class ServiceConfiguration(private val context: Context) {
    
    /**
     * Configuration data for the translator service
     */
    data class Config(
        val providerId: String,
        val targetLang: String,
        val redact: Boolean,
        val ttsEnabled: Boolean,
        val autoDetect: Boolean,
        val ttsPitch: Float,
        val ttsRate: Float,
        val ttsVoice: String?
    )
    
    /**
     * Load configuration from settings store
     */
    suspend fun loadConfig(settings: SettingsStore): Config {
        val prefs = settings.data.firstOrNull()
        
        return Config(
            providerId = prefs?.get(Keys.provider) ?: "fake",
            targetLang = prefs?.get(Keys.targetLang) ?: "es",
            redact = prefs?.get(Keys.redact) ?: false,
            ttsEnabled = prefs?.get(Keys.tts) ?: false,
            autoDetect = prefs?.get(Keys.autoLangDetect) ?: true,
            ttsPitch = prefs?.get(Keys.ttsPitch) ?: 1.0f,
            ttsRate = prefs?.get(Keys.ttsRate) ?: 1.0f,
            ttsVoice = prefs?.get(Keys.ttsVoice)
        )
    }
    
    /**
     * Create translation provider based on configuration
     */
    fun createTranslationProvider(): TranslationProvider {
        return try {
            MlKitTranslationProvider(context)
        } catch (e: Exception) {
            FakeTranslationProvider()
        }
    }
    
    /**
     * Create ASR provider based on configuration
     */
    fun createAsrProvider(providerId: String): AsrProvider {
        return when (providerId) {
            "whisper" -> {
                if (WhisperModelManager.isPresent(context)) {
                    WhisperCppProvider(context)
                } else {
                    FakeAsrProvider() // Fallback if model not available
                }
            }
            "system" -> {
                if (android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                    SystemSpeechRecognizerProvider(context)
                } else {
                    FakeAsrProvider() // Fallback if system ASR not available
                }
            }
            else -> FakeAsrProvider()
        }
    }
    
    /**
     * Validate configuration and return any issues
     */
    fun validateConfig(config: Config): List<String> {
        val issues = mutableListOf<String>()
        
        if (config.providerId == "whisper" && !WhisperModelManager.isPresent(context)) {
            issues.add("Whisper model not available")
        }
        
        if (config.providerId == "system" && 
            !android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
            issues.add("System speech recognizer not available")
        }
        
        if (config.targetLang.isBlank()) {
            issues.add("Target language not specified")
        }
        
        if (config.ttsPitch < 0.5f || config.ttsPitch > 2.0f) {
            issues.add("TTS pitch out of range (0.5-2.0)")
        }
        
        if (config.ttsRate < 0.5f || config.ttsRate > 2.0f) {
            issues.add("TTS rate out of range (0.5-2.0)")
        }
        
        return issues
    }
}