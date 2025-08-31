# Transcriber-android-app

Project scaffold for an offline-first Android + Wear OS translator.

- Product brief: `README.wristlingo.md`
- Architecture: `docs/architecture.md`
- Provider Contracts: `docs/providers.md`
- Testing: `docs/testing.md`
- Privacy & Permissions: `docs/privacy-and-permissions.md`

## Build

- Requirements: Android SDK (API 35), Build-Tools 35.0.0. The project auto-provisions a JDK 17 toolchain via the Foojay resolver plugin; you can run with Java 21 or 17.
- Local SDK path: make sure `local.properties` has `sdk.dir=/absolute/path/to/Android/Sdk`.

Commands:
- Debug (all flavors): `./gradlew :app:assembleDebug :wear:assembleDebug`
- Release (unsigned by default): `./gradlew :app:assembleRelease :wear:assembleRelease`

Artifacts are under `app/build/outputs/apk/**` and `wear/build/outputs/apk/**`.

## Quick test drive

Phone:
- Open `WristLingo` and configure at the top:
  - ASR Provider: `Fake` (no mic) or `System` (Android SpeechRecognizer, uses mic).
  - Target language (e.g., `es`).
  - Toggles: Redact PII, TTS.
- Tap Start. On Android 13+, allow Notifications. If you chose `System`, also allow Microphone.
- You’ll see a persistent notification and live captions. Tap Stop to end.

Wear:
- Launch `WristLingo` on the watch. Use Start/Stop to control the phone. Captions arrive in near‑real time.

## Release signing

Both modules read optional signing config from `keystore.properties` at the repo root. Template: `keystore.properties.example`.

1) Create a keystore (one-time):
```
keytool -genkeypair -v -keystore keystore/release.jks -alias release \
  -keyalg RSA -keysize 2048 -validity 10000
```
2) Create `keystore.properties` (do not commit):
```
storeFile=keystore/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=release
keyPassword=YOUR_KEY_PASSWORD
```
3) Build signed release:
```
./gradlew :app:assembleRelease :wear:assembleRelease
```

## CI (GitHub Actions)

Workflow: `.github/workflows/android.yml`.

- Builds all debug flavors on push/PR and uploads APK artifacts.
- Optional signed release on tags or manual dispatch if secrets are set:
  - `ANDROID_KEYSTORE_BASE64` — base64 of `keystore/release.jks`
  - `ANDROID_KEYSTORE_PASSWORD` — keystore store password
  - `ANDROID_KEY_ALIAS` — key alias (e.g., `release`)
  - `ANDROID_KEY_ALIAS_PASSWORD` — key password

Notes:
- CI overwrites `local.properties` to point at the CI SDK.
- If you prefer not to auto-download JDKs, install OpenJDK 17 and set `org.gradle.java.home` in `gradle.properties`.

## Implemented features

- Foreground service with two modes:
  - Fake ASR (no mic) + AudioRecord demo path
  - System SpeechRecognizer ASR (mic)
- Translation via ML Kit with on-demand model download/caching.
- Room database for Sessions and Utterances; optional PII redaction.
- TTS playback (phone) toggle.
- Data Layer messaging (MessageClient): phone broadcasts caption updates to Wear; Wear can start/stop sessions.
- Simple settings UI (provider, target language, redaction, TTS).

## Remaining work

- Whisper.cpp JNI provider (offline ASR) and selection.
- Export/import of sessions (JSONL) and redaction utilities UI.
- DataClient assets for large audio payloads; audio from watch→phone path.
- Full settings navigation and permissions education copy.
- Instrumented loopback tests and Compose snapshot tests.
