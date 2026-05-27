# Foreground Tasks

> Long-running, user-visible work for worker-kmp v3. Added in v3.0.0-alpha01
> (Phase 1 of the v3.0.0 epic). API marked `@RequiresOptIn(ExperimentalForegroundApi)`
> until v3.0.0 GA.

## When to use

Use `ForegroundWorker` (instead of `CoroutineWorker`) when work:
- Takes >30 seconds (exceeds default iOS background window)
- Must be user-visible (uploads, downloads, transcodes, syncs)
- Should survive app backgrounding

## Per-platform behavior

| Platform | Mechanism | OS-managed? | Notification |
|----------|-----------|:-----------:|-------------|
| Android | `setForeground(ForegroundInfo)` via androidx.work | ✓ | Required (foreground service) |
| Android 14+ | + foregroundServiceType in Manifest | ✓ | Required + typed |
| iOS 17+ | BGContinuedProcessingTaskRequest | ✓ | Optional (system status indicator) |
| iOS 13-16 | BGProcessingTaskRequest + UNNotification shim | ✗ partial | Optional |
| Desktop | java.awt.SystemTray + supervisor keepAlive | ✗ (in-process) | Tray icon |
| Web | Persistent Service Worker + Notifications API | ✗ partial (browser eviction) | Required (for visibility) |

## Current state (v3.0.0-alpha01)

**API surface stable. Per-platform actual implementations are LOG-ONLY STUBS** —
`runAsForeground(...)` emits a kermit INFO log but does not actually promote the
worker. Full per-platform impls land in v3.0.0-alpha01.X follow-ups per the
Phase 1 sub-plan's Tier B-E task list.

## Consumer usage

```kotlin
@OptIn(ExperimentalForegroundApi::class)
class FileUploadWorker(context: WorkerContext) : ForegroundWorker(context) {
    override suspend fun doWork(): WorkResult {
        setForeground(ForegroundInfo(
            notificationId = 42,
            title = "Uploading file",
            message = "0%",
            progress = WorkProgress(0),
            serviceType = ForegroundServiceType.DATA_SYNC,
        ))
        // ... upload logic with periodic setForeground(...) updates
        return WorkResult.success()
    }
}
```

## Android Manifest (when Phase 1 alpha01.X lands)

```xml
<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:foregroundServiceType="dataSync"
    tools:replace="android:foregroundServiceType" />

<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

## iOS Info.plist (when Phase 1 alpha01.X lands)

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER).continued</string>
</array>
<key>UIBackgroundModes</key>
<array>
    <string>processing</string>
</array>
```

## See also

- Phase 1 sub-plan: `plan-layer/.../01-foreground-tasks.md`
- TRUE_BACKGROUND_MATRIX.md — per-platform background equivalence levels (alpha05)
