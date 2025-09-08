# Repository Map: WristLingo (Android + Wear OS)

**WristLingo** is an offline-first Android + Wear OS translation app that captures speech on the watch, processes it on the phone using on-device ASR (Whisper) and translation (ML Kit), and streams live captions back to the watch.

## Repository Structure

```
Transcriber-android-app/
├── app/                    # Main Android phone application
│   ├── src/main/
│   │   ├── java/com/example/wristlingo/
│   │   │   ├── providers/  # ASR & Translation providers
│   │   │   ├── data/       # Database & data management
│   │   │   ├── wear/       # Wear OS communication
│   │   │   └── ...         # Core app components
│   │   └── cpp/            # Native Whisper JNI integration
│   └── build.gradle.kts    # App module build config
├── wear/                   # Wear OS companion application
│   └── src/main/java/com/example/wristlingo/wear/
├── docs/                   # Architecture & design documentation
├── codex/                  # AI assistant prompts & guidance
├── gradle/                 # Gradle version catalog & wrapper
└── build.gradle.kts        # Root project configuration
```

## Modules Overview

### root
**Path:** `.`  
**Language:** kotlin  
**Type:** library  

**Purpose:** Standard module

**External Dependencies:** junit:junit:4.13.2
**Tests:** 1 test files

### wear
**Path:** `wear`  
**Language:** kotlin  
**Type:** library  

**Purpose:** uses Jetpack Compose UI

**Key Classes:** MainActivity
**Key Functions:** WearScreen, send, onCreate, onDestroy, onMessageReceived
**Entrypoints:** MainActivity
**External Dependencies:** compose.ui, play.services.wearable, org.robolectric:robolectric:4.11.1
**Tests:** 1 test files

### wristlingo
**Path:** `app`  
**Language:** kotlin+cpp  
**Type:** library  

**Purpose:** contains background services; implements provider pattern; includes database layer; uses Jetpack Compose UI; integrates Whisper ASR; includes native C++ code

**Key Classes:** App, SimpleVad, State, WearBridge, TranslatorService
**Key Functions:** StatusHeader, handleStartService, onStop, AsrStateIndicator, onMessageReceived
**Entrypoints:** MainActivity
**External Dependencies:** androidx.room.ktx, androidx.room:room-testing:2.6.1, androidx.lifecycle.runtime.ktx
**Internal Dependencies:** wear
**Tests:** 5 test files

## Module Dependencies

```mermaid
graph TD
  root["root"]
  wear["wear"]
  wristlingo["wristlingo"]
  wristlingo --> wear
```

## Build & Runtime Requirements

**Build Requirements:**
- Android SDK (API 35)
- Build-Tools 35.0.0
- JDK 17+ (auto-provisioned via Foojay resolver)
- NDK (for Whisper C++ integration)

**Runtime Requirements:**
- Phone: Android 8.0+ (API 26+)
- Watch: Wear OS 4+

**Build Commands:**
```bash
# Debug builds (all flavors)
./gradlew :app:assembleDebug :wear:assembleDebug

# Release builds
./gradlew :app:assembleRelease :wear:assembleRelease

# Run tests
./gradlew :app:testOfflineDebug
```

## Product Flavors

The app supports three build flavors for different usage scenarios:

- **`offline`** - Whisper + ML Kit only (fully offline)
- **`hybrid`** - System SpeechRecognizer/Whisper + Cloud Translation (toggle)
- **`cloudstt`** - Enables Cloud STT v2 option (explicit opt-in)

## Architecture Highlights

**Data Flow:**
1. Watch captures audio via microphone
2. Small PCM frames (200-300ms) sent to phone via Data Layer
3. Phone processes audio with ASR (Whisper/System)
4. Text translated using ML Kit (on-device)
5. Captions streamed back to watch in real-time
6. Sessions stored in Room database for review

**Key Patterns:**
- **Provider Pattern:** Pluggable ASR and Translation providers
- **Repository Pattern:** Data access abstraction with Room
- **Foreground Service:** Background processing with proper notifications
- **Data Layer Messaging:** Watch-phone communication via MessageClient

## Complexity Hotspots

Files requiring attention due to size or complexity:

- **app/src/androidTest/java/com/example/wristlingo/integration/EndToEndDataFlowTest.kt** (314 LOC) - large file
- **app/src/androidTest/java/com/example/wristlingo/integration/EndToEndDataFlowTest.kt** (314 LOC) - large file
- **app/src/main/java/com/example/wristlingo/TranslatorService.kt** (258 LOC) - large file
- **app/src/main/java/com/example/wristlingo/TranslatorService.kt** (258 LOC) - large file
- **app/src/androidTest/java/com/example/wristlingo/integration/PhoneWearIntegrationTest.kt** (249 LOC) - large file
- **app/src/androidTest/java/com/example/wristlingo/integration/PhoneWearIntegrationTest.kt** (249 LOC) - large file
- **app/src/main/java/com/example/wristlingo/MainActivity.kt** (210 LOC) - large file
- **app/src/main/java/com/example/wristlingo/MainActivity.kt** (210 LOC) - large file

## Security & Privacy

- **On-device by default:** ASR and translation happen locally
- **Explicit cloud opt-in:** Cloud features disabled by default
- **PII redaction:** Optional redaction of sensitive information
- **Proper permissions:** Microphone, foreground service, wake lock
- **Secure storage:** Room database with optional encryption

## Development Workflow

**Local Development:**
1. Clone repository and ensure Android SDK is configured
2. Create `local.properties` with SDK path
3. Optionally create `keystore.properties` for release signing
4. Run `./gradlew :app:assembleOfflineDebug :wear:assembleOfflineDebug`

**Testing:**
- Unit tests: `./gradlew :app:testOfflineDebug`
- Integration tests available for providers and data layer
- Manual testing via debug builds on paired devices

**CI/CD:**
- GitHub Actions workflow builds all flavors on push/PR
- Artifacts uploaded for manual testing
- Optional signed releases on tags with proper secrets

## Extension Points

**Adding New Providers:**
1. Implement `AsrProvider` or `TranslationProvider` interface
2. Add provider to DI configuration
3. Update settings UI for selection

**Adding New Features:**
- Export formats: Extend `Export.kt` with new serializers
- Audio processing: Add VAD or noise reduction in `audio/`
- UI components: Leverage Compose architecture
- Data Layer: Add new message types in `WearBridge.kt`
