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

The current build ships with a simple, fake end-to-end path so you can verify the app scaffolding without real ASR/translation models.

- Phone app only: launch `WristLingo`, press Start. You’ll be prompted for notifications on Android 13+. The foreground service will emit simulated captions every ~150 ms and display translated text like "Hello world [es]". Press Stop to end.
- Wear app is a minimal launcher for now.

No microphone access is used in this test mode.

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

## What’s missing (scaffold vs. product)

This repo is intentionally a skeleton. For a production-ready experience, the following are outstanding:

- Data Layer messaging between Wear and Phone (MessageClient + DataClient) per `docs/architecture.md`.
- Real `AsrProvider` implementations (Android SpeechRecognizer, Whisper.cpp JNI) and selection UI.
- Real `TranslationProvider` (ML Kit) with on-demand model downloads and caching.
- Foreground audio capture pipeline using `AudioRecord`, buffering, and backpressure control.
- Room database (Session, Utterance) with export/import and redaction utilities.
- Settings screens for provider selection, permissions, privacy toggles.
- TTS playback pipeline on Phone and Wear.
- Instrumented tests and snapshot tests per `docs/testing.md`.

For quick validation, a "fake" ASR and Translation are wired inside the foreground service to simulate streaming captions.
