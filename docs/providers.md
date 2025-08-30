# Provider contracts

## AsrProvider
```kotlin
interface AsrProvider : Closeable {
  data class Partial(val text: String, val isFinal: Boolean)
  fun start(sampleRate: Int, languageHint: String? = null)
  fun feedPcm(frame: ShortArray): Partial?
  fun finalizeStream(): String // final text
}
```

Implementations:

* `SystemSpeechRecognizerProvider` (wraps Android SpeechRecognizer with partial results).
* `WhisperCppProvider` (JNI bridge; Tiny/Small models selectable; offline).
* `CloudSttV2Provider` (REST; disabled unless flag set).

## TranslationProvider

```kotlin
interface TranslationProvider {
  suspend fun ensureModel(source: String?, target: String)
  suspend fun translate(text: String, source: String?, target: String): String
}
```

Implementations:

* `MlKitTranslationProvider` (download packs on demand).
* `CloudTranslationProvider` (REST; off by default).

