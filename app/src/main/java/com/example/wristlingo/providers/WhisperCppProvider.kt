package com.example.wristlingo.providers

import android.content.Context
import android.util.Log
import com.example.wristlingo.whisper.WhisperModelManager
import kotlinx.coroutines.*

/**
 * ASR Provider implementation using Whisper.cpp via JNI.
 * Supports offline speech recognition with configurable models.
 */
class WhisperCppProvider(private val context: Context) : AsrProvider {
    private var listener: ((AsrProvider.Partial) -> Unit)? = null
    private var handle: Long = 0
    private var sampleRate: Int = 16000
    private var isStarted: Boolean = false
    private var accumulatedFrames = mutableListOf<ShortArray>()
    private val native = WhisperCppNative()
    
    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        private const val TAG = "WhisperCppProvider"
        private const val FRAMES_BEFORE_PARTIAL = 10 // Process every N frames for partial results
    }

    override fun start(sampleRate: Int, languageHint: String?) {
        Log.i(TAG, "Starting Whisper ASR with sample rate: $sampleRate")
        
        this.sampleRate = sampleRate
        
        val model = WhisperModelManager.modelFile(context)
        if (!model.exists()) {
            throw IllegalStateException("Whisper model missing at: ${model.absolutePath}")
        }
        
        try {
            handle = native.nativeInit(model.absolutePath, sampleRate)
            if (handle == 0L) {
                val error = native.nativeGetLastError(handle)
                throw RuntimeException("Failed to initialize Whisper: $error")
            }
            
            if (!native.nativeIsReady(handle)) {
                throw RuntimeException("Whisper context not ready after initialization")
            }
            
            isStarted = true
            accumulatedFrames.clear()
            Log.i(TAG, "Whisper ASR started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Whisper ASR", e)
            handle = 0L
            throw e
        }
    }

    override fun feedPcm(frame: ShortArray): AsrProvider.Partial? {
        if (handle == 0L || !isStarted) {
            Log.w(TAG, "Cannot feed PCM: provider not started")
            return null
        }
        
        try {
            // Feed frame to native context
            native.nativeFeed(handle, frame)
            accumulatedFrames.add(frame.clone())
            
            // Generate partial results periodically
            if (accumulatedFrames.size >= FRAMES_BEFORE_PARTIAL) {
                processingScope.launch {
                    generatePartialResult()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error feeding PCM data", e)
            val error = native.nativeGetLastError(handle)
            if (error.isNotEmpty()) {
                Log.e(TAG, "Native error: $error")
            }
        }
        
        return null // Partial results are delivered asynchronously via listener
    }

    override fun finalizeStream(): String {
        if (handle == 0L || !isStarted) {
            Log.w(TAG, "Cannot finalize: provider not started")
            return ""
        }
        
        return try {
            val result = native.nativeFinalize(handle)
            Log.i(TAG, "Finalized transcription: $result")
            
            // Reset for next stream
            accumulatedFrames.clear()
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing stream", e)
            val error = native.nativeGetLastError(handle)
            Log.e(TAG, "Native error: $error")
            ""
        }
    }

    override fun setListener(listener: ((AsrProvider.Partial) -> Unit)?) {
        this.listener = listener
    }

    override fun close() {
        Log.i(TAG, "Closing Whisper ASR provider")
        
        processingScope.cancel()
        
        if (handle != 0L) {
            try {
                native.nativeClose(handle)
            } catch (e: Exception) {
                Log.w(TAG, "Error closing native context", e)
            }
            handle = 0L
        }
        
        isStarted = false
        accumulatedFrames.clear()
        listener = null
    }
    
    /**
     * Generate partial transcription result for accumulated frames.
     * This runs on a background thread to avoid blocking audio processing.
     */
    private suspend fun generatePartialResult() {
        if (!isStarted || handle == 0L) return
        
        try {
            // For now, we don't have true partial results from Whisper
            // We could implement a sliding window approach here
            val frameCount = accumulatedFrames.size
            val duration = (frameCount * accumulatedFrames.firstOrNull()?.size ?: 0) / sampleRate.toFloat()
            
            // Generate a simple partial indicator
            val partial = AsrProvider.Partial(
                text = "[processing ${String.format("%.1f", duration)}s]",
                isFinal = false
            )
            
            withContext(Dispatchers.Main) {
                listener?.invoke(partial)
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error generating partial result", e)
        }
    }
}
