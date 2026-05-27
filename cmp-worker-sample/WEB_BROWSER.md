# Compose-for-Web Wasm Browser Sample (v3.0.0-alpha07)

This file marks the path for the v3.0.0-alpha07 Compose-for-Web browser sample.
Currently `cmp-worker-sample/jsMain/WebSampleMain.kt` ships a Node.js sample
(headless — no DOM). The browser sample adds a `wasmJsBrowserMain` source set
running the SAME `SampleApp()` Composable via `CanvasBasedWindow`.

## Differences

| | Node.js sample (jsMain) | Wasm browser sample (wasmJsBrowserMain) |
|---|---|---|
| Runner | `./gradlew :cmp-worker-sample:jsNodeRun` | `./gradlew :cmp-worker-sample:wasmJsBrowserRun` |
| Compose UI | none (headless) | ✓ `CanvasBasedWindow` |
| Service Worker | n/a | registers `worker-kmp-foreground-sw.js` (when Phase 1 ships) |
| Web Push | n/a | registers push subscription (when Phase 9 ships) |
| Status | shipped | scaffold (full impl in alpha07) |
