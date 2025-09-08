<!--
Design note:
- Added small seams for testability only (no runtime behavior changes):
  - WearBridge now accepts injected MessageClient/NodeClient.
  - Export JSONL can write to an injected Writer.
  - TranslationPipeline.speakText made internal for direct testing.
  - Introduced SettingsApi interface to mock SettingsStore in UI tests.
-->

# WristLingo Testing Plan

This document maps critical-path requirements to tests added/updated.

- Foreground session lifecycle
  - Robolectric service tests to be added separately (TranslatorService lifecycle and notification) – starts/stops, enters foreground, emits AppBus state.

- ASR provider abstraction
  - `SystemSpeechRecognizerProviderIntentTest` ensures RecognizerIntent extras configured.
  - Existing fake/whisper tests provide guard coverage (non-functional whisper path exceptions handled).

- Translation pipeline
  - `TranslationPipelineTest`: language detect gating, redaction, non-final bypass of persistence, DB writes, wear broadcasts.

- Persistence (Room)
  - `SessionDaoTest`, `UtteranceDaoTest` use in-memory Room and verify ordering and null handling.

- Redaction utilities
  - `RedactorTest` covers email and phone masking; add params later if needed.

- Export JSONL
  - `ExportJsonlTest` uses in-memory DB and StringWriter; verifies ordering and escaping.

- Wear Data Layer bridge
  - `WearBridgeTest` injects fake clients; verifies caption broadcast path and node iteration.

- TTS
  - Covered indirectly via `TranslationPipeline.speakText` path; audio side effects are not asserted.

- Settings/ViewModels
  - SettingsPanel interactions are pure UI wiring to `SettingsApi`; compose tests can assert toggle calls (omitted for now to keep suite fast).

- Coverage
  - JaCoCo wired with app unit test threshold >= 0.90; CI enforces and uploads HTML/XML.

Notes
- All tests are deterministic; no network used. ML Kit calls mocked via interfaces.

