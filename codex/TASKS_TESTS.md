```text
Strengthen tests and CI:

- Add JVM unit tests for Data Layer codecs: fuzz frames of variable length; corrupted headers; out-of-order sequences.
- Add instrumented tests for watch↔phone message roundtrip on emulators (skipped in CI if no emulators).
- Add a FakeAsrProvider and FakeTranslationProvider to make TranslatorService deterministic in tests.
- Ensure `./gradlew :app:testOfflineDebug` passes headlessly.
- Add ktlint/detekt tasks; wire them into the CI build job.
```

