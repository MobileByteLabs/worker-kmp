# cmp-worker-web-push

> Web Push universal-browser background for worker-kmp.

## Current state (alpha06.X — Phase 9 alpha06.X)

- ✓ Module + Gradle wiring (full KMP target matrix)
- ✓ `WebPushConfig` + `WebPushSubscription` + `WebPushSubscriber` interface + Koin module
- ✓ `WebPushConfig.serverEndpointAuthHeader: (suspend () -> String)?` — async auth-header
  provider for the subscription-POST
- ✓ Real JS `JsWebPushSubscriber` — `navigator.serviceWorker.register()` +
  `pushManager.subscribe({userVisibleOnly, applicationServerKey})` + ArrayBuffer →
  BASE64URL key encoding + auto-POST subscription to consumer's `serverEndpoint`
- ✓ Real `worker-kmp-sw.js` — push + periodicsync handlers, reads IndexedDB, filters
  ENQUEUED by scope, marks RUNNING, broadcasts on `BroadcastChannel('worker-kmp')`
- ✓ BroadcastChannel cross-tab wiring in `cmp-worker-web`'s `WebWorkManager` (JS only;
  WasmJs + JVM no-op)
- ✓ Reference push servers — `samples/web-push-server-node/` (Express + better-sqlite3 +
  web-push npm) + `samples/web-push-server-ktor/` (Ktor + Exposed + nl.martijndwars:web-push)
- ✓ `./gradlew :cmp-worker-web-push:generateVapidKeys` — instructions task
- 🕒 Real WasmJs `WebPushSubscriber` via kotlinx-browser bindings — alpha06.X.Y
- 🕒 BouncyCastle-based in-task VAPID key generator — alpha06.X.Y
- 🕒 End-to-end integration test against real browser — alpha06.X.Y

## Coordinates

`io.github.mobilebytelabs:worker-web-push:3.0.0-alpha06.X` (Maven Central — pending the
alpha06.X publication).

## See also

- [docs/features/web-push-server.md](../docs/features/web-push-server.md) (Phase 10) — RFC 8030 protocol + reference servers
- [docs/platform-support/true-background-matrix.md](../docs/platform-support/true-background-matrix.md) — per-browser Web Push capability levels
- Phase 9 sub-plan: `plan-layer/.../09-web-push-background.md`
- `samples/web-push-server-node/` + `samples/web-push-server-ktor/` — reference impls
