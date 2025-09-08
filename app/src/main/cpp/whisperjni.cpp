#include <jni.h>
#include <android/log.h>
#include <memory>
#include <vector>
#include "whisper_context.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "whisperjni", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "whisperjni", __VA_ARGS__)

// Convert jlong handle to WhisperContext pointer
static WhisperContext* getContext(jlong handle) {
    return reinterpret_cast<WhisperContext*>(handle);
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
    
    // Return raw pointer as handle (context is managed by shared_ptr)
    // We need to keep the shared_ptr alive, so we create a new one on the heap
    auto* contextPtr = new std::shared_ptr<WhisperContext>(context);
    return reinterpret_cast<jlong>(contextPtr->get());
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
        WhisperContext* context = getContext(handle);
        LOGI("Closing WhisperContext");
        // The context is managed by shared_ptr, so it will be cleaned up automatically
        // We just need to reset any internal state
        if (context) {
            context->reset();
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

