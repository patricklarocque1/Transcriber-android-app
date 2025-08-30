# Testing

## Unit
- Data Layer codecs: header encode/decode; fragmentation/sequence ordering.
- Repositories: Room read/write; pruning; search queries.
- Providers: fakes for ASR/Translation to test orchestrator logic.

## Instrumented
- Watch→Phone message loopback on emulator pair: measure latency budget.
- ForegroundService lifecycle: start/stop; survives screen off.
- Settings toggles: provider selection and ML Kit pack download.

## Snapshot
- Compose screens: watch tile, phone session list, caption overlay.

## Metrics (debug-only)
- Rolling average chars/min (for cloud cost estimator).

