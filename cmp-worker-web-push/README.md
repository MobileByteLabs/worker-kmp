# cmp-worker-web-push

> Web Push universal-browser background for worker-kmp. Lands at v3.0.0-alpha06.

## Current state (alpha06 scaffold)

- ✓ Module + Gradle wiring (full KMP target matrix)
- ✓ WebPushConfig + WebPushSubscription + WebPushSubscriber interface + Koin module
- ✓ Service Worker JS template (push + periodicsync event handlers)
- ✓ Stub per-platform actuals (log-only on JVM/iOS/JS/WasmJs)
- ✓ Smoke tests (2/2 GREEN)
- 🕒 Real JS pushManager.subscribe() + SW registration — alpha06.X
- 🕒 Real WasmJs @JsFun bindings — alpha06.X
- 🕒 IndexedDB read + work dispatch from SW context — alpha06.X
- 🕒 BroadcastChannel cross-tab dedup — alpha06.X
- 🕒 Reference push servers (Node.js + Ktor) — alpha06.X
- 🕒 VAPID key generation Gradle task — alpha06.X

## Coordinates

`io.github.mobilebytelabs:worker-web-push:3.0.0-alpha06` (Maven Central).

## See also

- [docs/features/web-push-server.md](../docs/features/web-push-server.md) (alpha06.X) — RFC 8030 protocol + reference servers
- [docs/platform-support/true-background-matrix.md](../docs/platform-support/true-background-matrix.md) — per-browser Web Push capability levels
- Phase 9 sub-plan: `plan-layer/.../09-web-push-background.md`
