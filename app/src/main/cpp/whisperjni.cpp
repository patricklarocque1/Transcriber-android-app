#include <jni.h>
#include <android/log.h>
#include <memory>
#include <vector>
#include "whisper_context.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "whisperjni", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "whisperjni", __VA_ARGS__)

/*
 * Memory Management Strategy:
 * 
 * - nativeInit: Creates a shared_ptr<WhisperContext> on the heap and returns its address as jlong handle
 * - Other functions: Use the handle to access the shared_ptr and the contained WhisperContext
 * - nativeClose: Deletes the heap-allocated shared_ptr, which automatically cleans up the WhisperContext
 * 
 * This approach ensures:
 * 1. No memory leaks - the shared_ptr wrapper is properly deleted
 * 2. Safe access - null checks prevent crashes from invalid handles
 * 3. RAII - WhisperContext destructor is called when shared_ptr is deleted
 */

// Convert jlong handle to shared_ptr<WhisperContext> pointer
static std::shared_ptr<WhisperContext>* getContextPtr(jlong handle) {
    if (handle == 0) {
        return nullptr;
    }
    return reinterpret_cast<std::shared_ptr<WhisperContext>*>(handle);
}

// Convert jlong handle to WhisperContext raw pointer for convenience
static WhisperContext* getContext(jlong handle) {
    auto* contextPtr = getContextPtr(handle);
    return (contextPtr && *contextPtr) ? contextPtr->get() : nullptr;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeInit(JNIEnv* env, jobject thiz, jstring modelPath, jint sampleRate) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Whisper native init with model: %s, sr=%d", path, sampleRate);
    
    WhisperContext::Config config;
    config.modelPath = std::string(path);
    config.sampleRate = sampleRate;
    
    env->ReleaseStringUTFChars(modelPath, path);
    
    auto context = WhisperContext::create(config);
    if (!context) {
        LOGE("Failed to create WhisperContext");
        return 0;
    }
    
    // Store the shared_ptr on the heap and return its address as handle
    // This allows us to properly manage the shared_ptr lifecycle
    auto* contextPtr = new std::shared_ptr<WhisperContext>(context);
    return reinterpret_cast<jlong>(contextPtr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeFeed(JNIEnv* env, jobject thiz, jlong handle, jshortArray pcm) {
    WhisperContext* context = getContext(handle);
    if (!context) {
        LOGE("Invalid context handle");
        return;
    }
    
    jsize length = env->GetArrayLength(pcm);
    jshort* elements = env->GetShortArrayElements(pcm, nullptr);
    
    if (elements) {
        std::vector<int16_t> samples(elements, elements + length);
        
        if (!context->feedAudio(samples)) {
            LOGE("Failed to feed audio: %s", context->getLastError().c_str());
        }
        
        env->ReleaseShortArrayElements(pcm, elements, JNI_ABORT);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeFinalize(JNIEnv* env, jobject thiz, jlong handle) {
    WhisperContext* context = getContext(handle);
    if (!context) {
        LOGE("Invalid context handle");
        return env->NewStringUTF("");
    }
    
    std::string result = context->finalize();
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeClose(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle != 0) {
        LOGI("Closing WhisperContext");
        
        // Get the shared_ptr from the handle and delete it to prevent memory leak
        auto* contextPtr = getContextPtr(handle);
        if (contextPtr) {
            // Reset any internal state before destruction
            if (*contextPtr) {
                (*contextPtr)->reset();
            }
            // Delete the heap-allocated shared_ptr to fix the memory leak
            // This is the key fix - we must delete the shared_ptr wrapper
            delete contextPtr;
            LOGI("WhisperContext shared_ptr deleted successfully");
        } else {
            LOGI("WhisperContext handle was already null or invalid");
        }
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeIsReady(JNIEnv* env, jobject thiz, jlong handle) {
    WhisperContext* context = getContext(handle);
    if (!context) {
        return JNI_FALSE;
    }
    return context->isReady() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeGetLastError(JNIEnv* env, jobject thiz, jlong handle) {
    WhisperContext* context = getContext(handle);
    if (!context) {
        return env->NewStringUTF("Invalid context handle");
    }
    return env->NewStringUTF(context->getLastError().c_str());
}

