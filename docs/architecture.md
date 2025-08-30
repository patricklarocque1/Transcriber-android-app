# Architecture

## Data path (default “offline” mode)
Watch mic → small PCM frames (16 kHz, 16-bit mono, ~200–300 ms per frame) → Data Layer `MessageClient` → Phone ForegroundService → ASR (Whisper JNI) → Translation (ML Kit) → captions/TTS back to watch (MessageClient) → Room log.

## Data Layer message schema (binary-first, with JSON fallback)
- Topic: `audio/pcm` (watch→phone), payload = header + PCM frame
  - Header (JSON, tiny): { "sr": 16000, "bits": 16, "seq": <u32>, "end": bool }
- Topic: `caption/update` (phone→watch): { "seq": <u32>, "text": "..." , "lang": "xx" }
- Topic: `session/control`: { "cmd": "start|stop|ping|pong" }

Keep messages under ~10KB. For large chunks or recordings, use `DataClient` with Assets.

## Providers (pluggable)
`AsrProvider`:
- `transcribe(audioFrame: ShortArray, sampleRate: Int): PartialResult`
- `finish(): FinalResult`

`TranslationProvider`:
- `translate(text: String, sourceLang: String?, targetLang: String): String`

Wire with DI (Hilt or Koin). Choose providers at runtime via Settings.

## Storage
- Room entities: `Session`, `Utterance` (id, sessionId, ts, srcText, dstText, lang, audioPath?).
- Export/Import via simple JSONL.

## TTS
- Use Android TTS on phone and Wear TTS on watch for short responses. Keep < 2s clips.

## Power & latency
- Audio frames 200–300 ms; backpressure via a bounded queue on the phone.
- ForegroundService + partial wakelock while actively capturing.

