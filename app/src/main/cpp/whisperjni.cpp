#include <jni.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "whisperjni", __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeInit(JNIEnv* env, jobject thiz, jstring modelPath, jint sampleRate) {
    const char* path = env->GetStringUTFChars(modelPath, 0);
    LOGI("Whisper native init with model: %s, sr=%d", path, sampleRate);
    env->ReleaseStringUTFChars(modelPath, path);
    // Return a fake handle
    return 1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeFeed(JNIEnv* env, jobject thiz, jlong handle, jshortArray pcm) {
    // Stub: do nothing
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_wristlingo_providers_WhisperCppNative_nativeFinalize(JNIEnv* env, jobject thiz, jlong handle) {
    const char* out = "[whisper-stub]";
    return env->NewStringUTF(out);
}

