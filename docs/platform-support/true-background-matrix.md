# True Background Matrix

> Per-platform background-execution capability levels for worker-kmp. The "true
> background" question: does work continue when the user closes the app?
>
> Phase 8 (Desktop daemon) makes this matrix accurate for Desktop. Phase 9
> (Web Push) makes it accurate for non-Chrome browsers. Until those land, this
> file is a SCAFFOLD documenting the target shape.

## TrueBackgroundLevel enum (lands in BackgroundCapabilities alpha05)

```kotlin
enum class TrueBackgroundLevel {
    NONE,                                // No background — app must be open
    APP_OPEN_ONLY,                        // In-process only
    BROWSER_OR_OS_BEST_EFFORT,            // iOS BGTask / Web SW / Web Push (varies by OS)
    OS_SCHEDULED_PERSISTENT,              // Survives close + reboot + (where applicable) logout
}
```

## Target matrix (v3.0.0 GA)

| Platform | Mechanism | trueBackgroundLevel | Survives close | Survives reboot |
|----------|-----------|---------------------|:---:|:---:|
| Android | WorkManager + JobScheduler | OS_SCHEDULED_PERSISTENT | ✓ | ✓ |
| iOS | BGTaskScheduler | BROWSER_OR_OS_BEST_EFFORT | ✓ | ✓ |
| Desktop (Win + Task Scheduler) | cmp-worker-desktop-daemon (Phase 8) | OS_SCHEDULED_PERSISTENT | ✓ | ✓ |
| Desktop (macOS + launchd) | cmp-worker-desktop-daemon (Phase 8) | OS_SCHEDULED_PERSISTENT | ✓ | ✓ (pauses on logout) |
| Desktop (Linux + systemd) | cmp-worker-desktop-daemon (Phase 8) | OS_SCHEDULED_PERSISTENT | ✓ | ✓ (pauses without linger) |
| Web (Chrome PWA + Push) | cmp-worker-web-push (Phase 9) | BROWSER_OR_OS_BEST_EFFORT | ✓ (browser eviction) | ✓ |
| Web (Firefox + Push) | cmp-worker-web-push (Phase 9) | BROWSER_OR_OS_BEST_EFFORT | ✓ | ✓ |
| Web (Safari 16.4+ PWA + Push) | cmp-worker-web-push (Phase 9) | BROWSER_OR_OS_BEST_EFFORT | partial | ✓ |
| Web (older browsers) | tab-open polling fallback | APP_OPEN_ONLY | ✗ | ✗ |

## Current state (alpha01)

The TrueBackgroundLevel enum + BackgroundCapabilities.trueBackgroundLevel field
land at alpha05 (Phase 8). Until then, BackgroundCapabilities exposes the v2.1.0
2-field shape (supportsPersistence + supportsOsScheduling).
