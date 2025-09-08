package com.example.wristlingo.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.wristlingo.AppBus
import com.example.wristlingo.data.Redactor
import com.example.wristlingo.data.db.AppDatabase
import com.example.wristlingo.data.db.Utterance
import com.example.wristlingo.lang.LanguageId
import com.example.wristlingo.providers.TranslationProvider
import com.example.wristlingo.wear.WearBridge

/**
 * Handles the text processing pipeline: language detection, translation, 
 * PII redaction, TTS, and data persistence.
 */
class TranslationPipeline(
    private val context: Context,
    private val translationProvider: TranslationProvider,
    private val db: AppDatabase,
    private val wearBridge: WearBridge,
    private val langId: LanguageId
) {
    private var tts: TextToSpeech? = null
    private var lastUtteranceHash: Int? = null
    
    companion object {
        private const val TAG = "TranslationPipeline"
    }
    
    /**
     * Configuration for the translation pipeline
     */
    data class Config(
        val targetLang: String,
        val redact: Boolean,
        val ttsEnabled: Boolean,
        val autoDetect: Boolean,
        val ttsPitch: Float = 1.0f,
        val ttsRate: Float = 1.0f,
        val ttsVoice: String? = null
    )
    
    fun initializeTts(config: Config) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureTts(config)
            }
        }
    }
    
    private fun configureTts(config: Config) {
        try {
            tts?.setPitch(config.ttsPitch)
            tts?.setSpeechRate(config.ttsRate)
            
            val locale = java.util.Locale.forLanguageTag(config.targetLang)
            tts?.language = locale
            
            // Set voice if specified
            config.ttsVoice?.let { voiceName ->
                val voice = tts?.voices?.firstOrNull { it.name == voiceName }
                    ?: tts?.voices?.firstOrNull { 
                        it.locale?.toLanguageTag()?.startsWith(config.targetLang, ignoreCase = true) == true 
                    }
                voice?.let { tts?.voice = it }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring TTS", e)
        }
    }
    
    /**
     * Process text through the translation pipeline
     */
    suspend fun processText(
        text: String, 
        isFinal: Boolean, 
        sessionId: Long,
        config: Config
    ) {
        try {
            // Detect source language if enabled
            val sourceLang = if (config.autoDetect && isFinal) {
                try {
                    langId.detect(text)
                } catch (e: Exception) {
                    Log.w(TAG, "Language detection failed", e)
                    null
                }
            } else null
            
            // Apply PII redaction
            val cleanText = if (config.redact) {
                Redactor.redact(text)
            } else text
            
            // Translate text
            val translatedText = try {
                translationProvider.translate(cleanText, source = sourceLang, target = config.targetLang)
            } catch (e: Exception) {
                Log.w(TAG, "Translation failed, using original text", e)
                cleanText
            }
            
            // Broadcast to UI and wear
            AppBus.captions.tryEmit(translatedText)
            wearBridge.broadcastCaption(translatedText)
            
            // Persist to database
            if (isFinal) {
                persistUtterance(sessionId, cleanText, translatedText, config.targetLang)
            }
            
            // Speak if TTS enabled
            if (isFinal && config.ttsEnabled) {
                speakText(translatedText)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing text", e)
            // Fallback: at least broadcast the original text
            AppBus.captions.tryEmit(text)
        }
    }
    
    private suspend fun persistUtterance(
        sessionId: Long, 
        srcText: String, 
        dstText: String, 
        targetLang: String
    ) {
        try {
            db.utteranceDao().insert(
                Utterance(
                    sessionId = sessionId,
                    ts = System.currentTimeMillis(),
                    srcText = srcText,
                    dstText = dstText,
                    lang = targetLang
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting utterance", e)
        }
    }
    
    private fun speakText(text: String) {
        val textHash = text.hashCode()
        if (textHash != lastUtteranceHash) {
            try {
                tts?.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "utt-${System.currentTimeMillis()}"
                )
                lastUtteranceHash = textHash
            } catch (e: Exception) {
                Log.w(TAG, "Error speaking text", e)
            }
        }
    }
    
    fun shutdown() {
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
    }
}