```text
Wire cost-aware cloud toggles without breaking offline mode:

- Add CloudTranslationProvider using Google Translation v3 REST; inject API key via BuildConfig or env. Wrap in try/catch; fall back to ML Kit on failure.
- Add CloudSttV2Provider stub with API surface but disabled unless feature flag true AND key presented.
- Implement a CostEstimator: rolling chars/min estimate from recent transcripts; show an estimate banner in Settings with Cloud Translation cost (assume $20/million chars after free tier) and Cloud STT v2 cost ($0.016/min). This is UI-only; no billing code.
- Add integration tests that assert offline flavor builds and runs with zero cloud deps.
```

