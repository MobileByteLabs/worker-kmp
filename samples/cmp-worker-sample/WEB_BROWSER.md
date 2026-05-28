# Compose-for-Web Wasm Browser Sample (v3.0.0-alpha07)

This file tracks the v3.0.0-alpha07 Compose-for-Web browser sample.
`cmp-worker-sample/jsMain/WebSampleMain.kt` ships a Node.js sample
(headless — no DOM). The browser sample adds a `wasmJsBrowserMain` source set
running in the browser; full Compose-for-Wasm UI (`CanvasBasedWindow` +
`SampleApp()`) lands in alpha07.X.Y.

## Differences

| | Node.js sample (jsMain) | Wasm browser sample (wasmJsBrowserMain) |
|---|---|---|
| Runner | `./gradlew :cmp-worker-sample:jsNodeRun` | `./gradlew :cmp-worker-sample:wasmJsBrowserRun` |
| Compose UI | none (headless) | scaffold (alpha07) — full `CanvasBasedWindow` in alpha07.X.Y |
| Service Worker | n/a | registers `worker-kmp-foreground-sw.js` (Phase 1 — already shipped) |
| Web Push | n/a | registers push subscription (Phase 9 — already shipped) |
| Status | shipped | scaffold shipped (alpha07); full Compose UI in alpha07.X.Y |

## What ships in alpha07

- **`wasmJs { browser { ... } }` target** added to `cmp-worker-sample/build.gradle.kts`
  with `binaries.executable()` + webpack `cssSupport` + named output `cmp-worker-sample.js`.
- **`src/wasmJsMain/kotlin/BrowserSampleMain.kt`** — plain-DOM entry point
  (`fun main()`) that injects an enqueue button + event log into `index.html`.
  Demonstrates the worker-kmp Wasm import + DOM event wiring; the actual
  `WebWorkManager.enqueue(...)` call defers to alpha07.X.Y.
- **`src/wasmJsMain/resources/index.html`** — host page loading the Wasm bundle.

## Run

```bash
./gradlew :cmp-worker-sample:wasmJsBrowserRun
```

Webpack-dev-server boots; navigate to `http://localhost:8080`.

## Deferred to alpha07.X.Y

- Full Compose-for-Wasm UI via `CanvasBasedWindow("worker-kmp") { SampleApp() }`
  — requires lifting `SampleApp()` from `commonMain` and resolving any non-wasm
  dependencies (compose.desktop is desktop-only).
- Real `WebWorkManager.enqueue(...)` wiring on button click — currently log-only.
- iOS Xcode sample + Hilt Android sample — README-only scaffolds today;
  the full Xcode project + APK defer to alpha07.X.Y based on consumer need.
