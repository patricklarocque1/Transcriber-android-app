# WristLingo (Android + Wear OS)

Hands-free, on-the-go translation. The Wear OS app captures nearby speech and streams it to the phone. The phone performs on-device ASR (speech-to-text) and on-device translation by default, then streams live captions + short TTS back to the watch. Sessions are saved for review even if the apps aren’t foregrounded.

## Core goals
- **Watch-first capture**: push-to-talk and quick phrases on Wear OS; stream small PCM frames to phone via the Android Data Layer.
- **Phone processing**: foreground service runs **on-device** ASR (Whisper via `whisper.cpp` JNI or Android SpeechRecognizer) + **on-device** translation (ML Kit). Optional cloud fallbacks (Google Cloud Translation; Cloud STT v2) are opt-in.
- **Review later**: timestamped logs in Room DB with search/export; optional audio snippets.
- **Privacy**: explicit mic indicators; on-device by default; cloud usage is transparent + disabled by default.

## Modules
- `:app` — Android phone app (Compose)
- `:wear` — Wear OS app (Compose for Wear OS)

## Min targets
- Phone: Android 8.0 (API 26+)
- Watch: Wear OS 4+ (target current)

## Build flavors (cost-aware)
- `offline`  — Whisper + ML Kit only
- `hybrid`   — System SpeechRecognizer/Whisper + Cloud Translation (toggle)
- `cloudstt` — Enables Cloud STT v2 option (explicit opt-in)

## Providers
- **ASR**: `SystemSpeechRecognizerProvider`, `WhisperCppProvider`, `CloudSttV2Provider` (flagged)
- **Translation**: `MlKitTranslationProvider`, `CloudTranslationProvider` (flagged)

## Quickstart
1. Open in Android Studio; sync Gradle.
2. Run `:wear` on your watch/emulator and `:app` on your phone/emulator (pair via Wear OS emulator pairing or physical devices).
3. Default flavor is `offline`. Use run configs or Gradle tasks for other flavors.
4. Push-to-talk on the watch; see live captions on the watch and log in the phone app.

## CLI builds
```bash
./gradlew :app:assembleOfflineDebug :wear:assembleOfflineDebug
./gradlew :app:testOfflineDebug
```

## Optional Cloud (off by default)

* **Translation**: Google Cloud Translation (free tier generous; toggle in Settings).
* **Speech**: Cloud STT v2 ($/min; toggle in Settings).
  Add keys as Gradle properties or environment variables only when testing cloud flows.

## Permissions & foreground service

* RECORD_AUDIO, FOREGROUND_SERVICE_MICROPHONE, WAKE_LOCK, BLUETOOTH (if needed), INTERNET (only for cloud mode).
* ForegroundService shows a persistent notification while capturing/processing.

## License

MIT (change as you like).

## Docs

- Architecture: `docs/architecture.md`
- Provider Contracts: `docs/providers.md`
- Testing: `docs/testing.md`
- Privacy & Permissions: `docs/privacy-and-permissions.md`

## Codex Prompts

- Bootstrap scaffold: `codex/BOOTSTRAP.md`
- Feature tasks: `codex/TASKS_FEATURES.md`
- Cloud guards: `codex/TASKS_CLOUD_GUARDS.md`
- Tests & CI hardening: `codex/TASKS_TESTS.md`
- Hotfix playbook: `codex/HOTFIX_PLAYBOOK.md`
