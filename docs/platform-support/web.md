---
title: "Web"
description: "worker-kmp on JS / wasmJs — Web Workers + Service Workers for offline / background; Push API integration."
---

# Web

worker-kmp on Web runs in two modes:

1. **Foreground / tab-bound** (default) — workers execute on a coroutine scheduler tied
   to the page lifetime. Suspends when tab is backgrounded; resumes on focus.
2. **True background via Web Push** (`cmp-worker-web-push`) — your consumer-owned server
   sends Web Push messages to a Service Worker that wakes the browser and dispatches the
   matching `CoroutineWorker`. Works across Chrome, Firefox, Safari 16.4+, and Edge.

## Requirements

- Any modern browser (Service Worker required for true-background)
- Browser permissions: **Notifications** (required for Web Push true-background)
- A **consumer-owned server** that handles the Web Push fan-out (see [Web Push Server Guide](../features/web-push-server.md))

## Dependency

```kotlin
// commonMain
api(libs.worker.kmp)
api(libs.worker.koin)

// jsMain + wasmJsMain
api(libs.worker.web)

// (optional) true-background via Web Push
implementation(libs.worker.web.push)
```

## Service Worker file

Place the worker-kmp Service Worker JS file at a stable URL — typically the site root so
its `scope` covers your whole origin:

```
/worker-kmp-sw.js     ← served at origin root, MIME type application/javascript
```

The file is shipped by `cmp-worker-web-push` as a resource — copy it to your web bundle
output (or use Gradle's `processResources` to wire it in).

Register the SW from commonMain (the library handles browser API access internally):

```kotlin
WebServiceWorkerRegistrar.register(scriptUrl = "/worker-kmp-sw.js", scope = "/")
```

## VAPID key generation (one-time per app)

```bash
./gradlew :cmp-worker-web-push:generateVapidKeys
# writes:
#   build/vapid/private-key.pem   ← upload to framework vault via /secrets push
#   build/vapid/public-key.txt    ← embed in your client app
```

The private key MUST live in your framework-managed vault per RULE-SECRETS-VAULT-001.
**Never** commit it; **never** paste it in chat. See [Security](../operations/security.md) T19-T21.

## Platform factory

```kotlin
// jsMain or wasmJsMain
fun startWorkerKoin() {
    startKoin {
        modules(
            workKoinModule(
                config = WorkerConfig(
                    logLevel = LogLevel.INFO,
                    webConfig = WebWorkManagerConfig(
                        vapidPublicKey = "BNQ...your-public-key-base64url...",
                        notificationPermissionAutoRequest = false,  // ask explicitly via UI
                        subscriptionExpiryDays = 90,
                    ),
                ),
                workers = workerRegistry { register<DataSyncWorker> { ctx -> DataSyncWorker(ctx, get()) } },
                factory = webWorkManagerFactory(),
            ),
        )
    }
}
```

## Persistence

The work queue is stored in **IndexedDB** under database name `worker-kmp` (override via
`WebWorkManagerConfig.persistenceDbName`). Schema validation runs on every read — see
[Security](../operations/security.md) T14.

## Browser support matrix (true background via Web Push)

| Browser | Min version | Notes |
|---|---|---|
| Chrome | 50 | Full support |
| Firefox | 44 | Full support |
| Safari | 16.4 (macOS 13+ / iOS 16.4+) | Apple Push Notification Service backend; PWA only on iOS |
| Edge | 17 | Full support |

See [True Background Matrix](true-background-matrix.md) for the cross-platform picture.

## See also

- [Web Push Server Guide](../features/web-push-server.md) — consumer server setup
- [Platform API Matrix](platform-api-matrix.md)
- [Security](../operations/security.md) — Web Push attack surfaces T7-T18
