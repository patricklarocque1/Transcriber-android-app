#pragma once

#include <vector>
#include <string>
#include <memory>
#include <mutex>

/**
 * Whisper context wrapper for JNI integration.
 * This is a stub implementation that simulates Whisper.cpp behavior
 * without requiring the actual Whisper.cpp library.
 */
class WhisperContext {
public:
    struct Config {
        std::string modelPath;
        int sampleRate;
        int maxTokens;
        float temperature;
        bool useGpu;
        
        Config() : sampleRate(16000), maxTokens(256), temperature(0.0f), useGpu(false) {}
    };

    static std::shared_ptr<WhisperContext> create(const Config& config);
    
    ~WhisperContext();

    // Audio processing
    bool feedAudio(const std::vector<float>& samples);
    bool feedAudio(const std::vector<int16_t>& samples);
    std::string finalize();
    void reset();
    
    // State management
    bool isReady() const;
    std::string getLastError() const;
    
    // Configuration
    void setTemperature(float temp);
    void setMaxTokens(int tokens);

private:
    WhisperContext(const Config& config);
    bool initialize();
    
    Config config_;
    bool initialized_;
    std::string lastError_;
    std::vector<float> audioBuffer_;
    std::mutex mutex_;
    
    // Stub processing
    std::string processAudioStub() const;
};