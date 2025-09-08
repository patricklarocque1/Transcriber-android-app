#include "whisper_context.h"
#include <android/log.h>
#include <algorithm>
#include <random>
#include <sstream>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "whisper_context", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "whisper_context", __VA_ARGS__)

std::shared_ptr<WhisperContext> WhisperContext::create(const Config& config) {
    auto context = std::shared_ptr<WhisperContext>(new WhisperContext(config));
    if (context->initialize()) {
        return context;
    }
    return nullptr;
}

WhisperContext::WhisperContext(const Config& config) 
    : config_(config), initialized_(false) {
    LOGI("Creating WhisperContext with model: %s", config_.modelPath.c_str());
}

WhisperContext::~WhisperContext() {
    LOGI("Destroying WhisperContext");
}

bool WhisperContext::initialize() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    // Stub initialization - just check if model file path is reasonable
    if (config_.modelPath.empty()) {
        lastError_ = "Model path is empty";
        LOGE("%s", lastError_.c_str());
        return false;
    }
    
    if (config_.sampleRate <= 0 || config_.sampleRate > 48000) {
        lastError_ = "Invalid sample rate: " + std::to_string(config_.sampleRate);
        LOGE("%s", lastError_.c_str());
        return false;
    }
    
    // Reserve space for audio buffer (10 seconds worth)
    audioBuffer_.reserve(config_.sampleRate * 10);
    
    initialized_ = true;
    LOGI("WhisperContext initialized successfully");
    return true;
}

bool WhisperContext::feedAudio(const std::vector<float>& samples) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (!initialized_) {
        lastError_ = "Context not initialized";
        return false;
    }
    
    // Append samples to buffer
    audioBuffer_.insert(audioBuffer_.end(), samples.begin(), samples.end());
    
    // Limit buffer size to prevent memory issues
    const size_t maxBufferSize = config_.sampleRate * 30; // 30 seconds
    if (audioBuffer_.size() > maxBufferSize) {
        audioBuffer_.erase(audioBuffer_.begin(), 
                          audioBuffer_.begin() + (audioBuffer_.size() - maxBufferSize));
    }
    
    return true;
}

bool WhisperContext::feedAudio(const std::vector<int16_t>& samples) {
    // Convert int16 to float
    std::vector<float> floatSamples;
    floatSamples.reserve(samples.size());
    
    for (int16_t sample : samples) {
        floatSamples.push_back(static_cast<float>(sample) / 32768.0f);
    }
    
    return feedAudio(floatSamples);
}

std::string WhisperContext::finalize() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (!initialized_) {
        lastError_ = "Context not initialized";
        return "";
    }
    
    if (audioBuffer_.empty()) {
        return "";
    }
    
    // Process accumulated audio (stub implementation)
    std::string result = processAudioStub();
    
    // Clear buffer for next session
    audioBuffer_.clear();
    
    LOGI("Finalized transcription: %s", result.c_str());
    return result;
}

void WhisperContext::reset() {
    std::lock_guard<std::mutex> lock(mutex_);
    audioBuffer_.clear();
    lastError_.clear();
}

bool WhisperContext::isReady() const {
    return initialized_;
}

std::string WhisperContext::getLastError() const {
    return lastError_;
}

void WhisperContext::setTemperature(float temp) {
    config_.temperature = std::max(0.0f, std::min(1.0f, temp));
}

void WhisperContext::setMaxTokens(int tokens) {
    config_.maxTokens = std::max(1, std::min(1024, tokens));
}

std::string WhisperContext::processAudioStub() const {
    // Stub implementation that generates plausible transcriptions
    // based on audio buffer characteristics
    
    if (audioBuffer_.empty()) {
        return "";
    }
    
    // Calculate some basic audio characteristics
    float avgAmplitude = 0.0f;
    float maxAmplitude = 0.0f;
    
    for (float sample : audioBuffer_) {
        float abs_sample = std::abs(sample);
        avgAmplitude += abs_sample;
        maxAmplitude = std::max(maxAmplitude, abs_sample);
    }
    avgAmplitude /= audioBuffer_.size();
    
    // Generate stub transcription based on audio characteristics
    std::vector<std::string> phrases = {
        "Hello, this is a test.",
        "The quick brown fox jumps over the lazy dog.",
        "How are you doing today?",
        "This is a sample transcription.",
        "Testing the Whisper integration.",
        "The weather is nice today.",
        "Can you hear me clearly?",
        "This is working as expected."
    };
    
    // Use audio characteristics to select phrase
    std::mt19937 rng(static_cast<unsigned>(audioBuffer_.size() + avgAmplitude * 1000));
    std::uniform_int_distribution<size_t> dist(0, phrases.size() - 1);
    
    std::string result = phrases[dist(rng)];
    
    // Add some variation based on audio characteristics
    if (avgAmplitude < 0.1f) {
        result = "[low volume] " + result;
    } else if (maxAmplitude > 0.8f) {
        result = "[loud] " + result;
    }
    
    return result;
}