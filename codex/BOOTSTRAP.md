> Run `codex` in your repo root, paste the block below as one message.

```text
You are an expert Android + Wear OS engineer. Scaffold a production-ready, offline-first translator app called "WristLingo" with two Gradle modules: :app (phone) and :wear (watch). Kotlin + Jetpack Compose, Gradle Kotlin DSL, JDK 21, AGP latest stable.

Constraints:
- NO Vertex AI.
- Offline-first: default providers are Whisper (JNI, tiny/small models selectable) and ML Kit on-device translation.
- Optional cloud: Google Cloud Translation (toggle), Cloud STT v2 (toggle). All cloud code must be behind interfaces and disabled by default.
- Product flavors: offline, hybrid, cloudstt.
- Min SDKs: phone 26+, watch Wear OS 4+ (target current).
- Permissions and ForegroundService with proper categories.

Deliverables:
1) Gradle multi-module setup (settings.gradle.kts; libs.versions.toml or version catalog; module build files).
2) App structure:
   - :app ForegroundService `TranslatorService`
   - DI (Hilt or Koin) wiring for `AsrProvider` and `TranslationProvider`
   - Room database (Session, Utterance) + repository + DAO + migrations
   - Compose screens: Sessions list, Session detail, Live caption overlay, Settings
   - TTS helper
3) :wear module:
   - Compose for Wear OS: main app screen with push-to-talk, quick phrases, rolling caption view
   - Data Layer integration (MessageClient for small frames; DataClient for bulk)
   - Wear Tile for quick access
4) Data Layer codecs and message schemas with tests:
   - audio/pcm (header+payload), caption/update, session/control
5) Providers (interfaces + default impls + fakes + tests):
   - AsrProvider: SystemSpeechRecognizerProvider, WhisperCppProvider (JNI stub), CloudSttV2Provider (stub, behind flag)
   - TranslationProvider: MlKitTranslationProvider, CloudTranslationProvider (stub, behind flag)
6) Settings screen toggles and ML Kit model download manager
7) Foreground notification, mic indicators, runtime permission flows
8) Minimal JNI bridge stub for Whisper (interface, native signatures, basic loader; do NOT include heavy binaries)
9) CI-friendly tasks:
   - `:app:assembleOfflineDebug :wear:assembleOfflineDebug :app:testOfflineDebug`

Add comments explaining tradeoffs and extension points. Provide README updates with build/run instructions and flavor usage. Do not add heavy dependencies; keep it lean and standard.
```

