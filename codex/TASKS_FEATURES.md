> Use this when you want Codex to implement missing pieces or refine.

```text
Implement these features incrementally:

1) Data Layer
- Create a small header struct (JSON) {sr, bits, seq, end} and a binary codec that prefixes header length (u16) then PCM shorts.
- Write unit tests for encode/decode and sequencing.

2) TranslatorService orchestrator
- Bounded queue for audio frames; coalesce when under load.
- Call AsrProvider.start(sr), feed frames, and emit partials every ~300 ms.
- On finalization, persist Utterance to Room; kick TranslationProvider; send caption/update messages back to the watch.

3) Providers
- SystemSpeechRecognizerProvider: partial results via listener; handle cancel/timeouts.
- WhisperCppProvider: JNI surface with start/feed/finalize; add a FakeWhisperProvider for tests.
- MlKitTranslationProvider: dynamic pack download; simple cache of availability.
- Cloud providers: stub classes with feature flags; gated behind settings; never invoked in offline flavor.

4) UI
- Wear: big PTT button, recording indicator, quick phrases (localized), rolling caption.
- Phone: sessions list (paging), detail view with timeline, export JSONL.

5) Settings
- Toggles: useOnDeviceAsr (default true), useCloudTranslate (default false), useCloudSttV2 (default false)
- Target language picker; on-demand model download.

6) Tests
- Repositories; database migrations; Data Layer codecs; Fake providers and orchestrator.
```

