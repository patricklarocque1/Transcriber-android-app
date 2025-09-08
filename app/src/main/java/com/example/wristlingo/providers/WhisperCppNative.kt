package com.example.wristlingo.providers

/**
 * JNI bridge to the native Whisper.cpp implementation.
 * This provides a low-level interface to the native Whisper context.
 */
class WhisperCppNative {
    
    companion object {
        init {
            try {
                System.loadLibrary("whisperjni")
            } catch (e: UnsatisfiedLinkError) {
                // Library not available - this is expected in some build variants
                android.util.Log.w("WhisperCppNative", "Native library not available: ${e.message}")
            }
        }
    }
    
    /**
     * Initialize a new Whisper context with the given model and sample rate.
     * @param modelPath Path to the Whisper model file
     * @param sampleRate Audio sample rate (typically 16000)
     * @return Native handle to the context, or 0 if initialization failed
     */
    external fun nativeInit(modelPath: String, sampleRate: Int): Long
    
    /**
     * Feed audio data to the Whisper context.
     * @param handle Native context handle
     * @param pcm Audio samples as 16-bit PCM data
     */
    external fun nativeFeed(handle: Long, pcm: ShortArray)
    
    /**
     * Finalize the current audio stream and get transcription.
     * @param handle Native context handle
     * @return Transcribed text, or empty string if no transcription available
     */
    external fun nativeFinalize(handle: Long): String
    
    /**
     * Close the Whisper context and clean up resources.
     * @param handle Native context handle
     */
    external fun nativeClose(handle: Long)
    
    /**
     * Check if the context is ready for processing.
     * @param handle Native context handle
     * @return true if ready, false otherwise
     */
    external fun nativeIsReady(handle: Long): Boolean
    
    /**
     * Get the last error message from the native context.
     * @param handle Native context handle
     * @return Error message, or empty string if no error
     */
    external fun nativeGetLastError(handle: Long): String
}