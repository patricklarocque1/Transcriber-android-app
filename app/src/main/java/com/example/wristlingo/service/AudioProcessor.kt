package com.example.wristlingo.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.wristlingo.audio.SimpleVad
import com.example.wristlingo.providers.AsrProvider
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

/**
 * Handles audio recording and processing for the TranslatorService.
 * Encapsulates VAD (Voice Activity Detection) and ASR provider interaction.
 */
class AudioProcessor(
    private val sampleRate: Int = 16000
) {
    private var audioRecord: AudioRecord? = null
    private val vad = SimpleVad()
    
    companion object {
        private const val TAG = "AudioProcessor"
    }
    
    /**
     * Start audio processing with the given ASR provider.
     * This function blocks until the coroutine context is cancelled.
     */
    suspend fun processAudio(
        asrProvider: AsrProvider,
        context: CoroutineContext,
        onFinalText: suspend (String) -> Unit
    ) {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setSampleRate(sampleRate)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .build()
        
        try {
            asrProvider.start(sampleRate, null)
            audioRecord?.startRecording()
            
            val buffer = ShortArray(1600)
            
            while (context.isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    val frame = buffer.copyOf(read)
                    processAudioFrame(frame, asrProvider, onFinalText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio", e)
        } finally {
            cleanup(asrProvider)
        }
    }
    
    private suspend fun processAudioFrame(
        frame: ShortArray,
        asrProvider: AsrProvider,
        onFinalText: suspend (String) -> Unit
    ) {
        when (vad.analyze(frame)) {
            SimpleVad.State.Start, SimpleVad.State.Speech -> {
                asrProvider.feedPcm(frame)
            }
            SimpleVad.State.End -> {
                val finalText = asrProvider.finalizeStream()
                if (finalText.isNotBlank()) {
                    onFinalText(finalText)
                }
                asrProvider.start(sampleRate, null)
            }
            else -> {
                // Silence - do nothing
            }
        }
    }
    
    private fun cleanup(asrProvider: AsrProvider) {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            asrProvider.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during cleanup", e)
        }
    }
}