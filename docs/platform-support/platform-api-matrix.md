---
title: "Platform API matrix"
description: "Side-by-side table of which worker-kmp APIs are supported / partially supported / not available per platform."
---

# Platform API Matrix

> Every native background-execution API × every platform that worker-kmp targets.
> v2.1.0 baseline. Per Phase 7 of the v3.0.0 epic.

## Legend

| Symbol | Meaning |
|:-:|---|
| ✓ | Fully supported via the worker-kmp API |
| ◐ | Partially supported (per-platform notes) |
| ✗ | Not available on this platform |
| 🕒 | Lands in a future alpha (per-cell note) |

## Core scheduling

| Native API | Android | iOS | Desktop | Web |
|------------|:-:|:-:|:-:|:-:|
| One-time work | ✓ | ✓ | ✓ | ✓ |
| Periodic work | ✓ | ✓ (BGAppRefresh — Phase 7 alpha04.X) | ✓ (in-process timer) | ◐ (Background Sync — Chrome only; Web Push — Phase 9) |
| Work chaining | ✓ | ✓ | ✓ | ✓ |
| Unique work names | ✓ | ✓ | ✓ | ✓ |

## Constraints

| Constraint | Android | iOS | Desktop | Web |
|------------|:-:|:-:|:-:|:-:|
| Network type (CONNECTED/UNMETERED/etc.) | ✓ | ✓ | ✓ | ✓ (navigator.onLine) |
| Charging | ✓ | ✓ (BGProcessingTask flag) | ◐ (JVM system properties) | ◐ (Battery Status API) |
| Battery not low | ✓ | ✓ | ◐ | ◐ |
| Storage not low | ✓ | ✗ | ◐ | ◐ (StorageManager.estimate) |
| Device idle | ✓ | ✗ | ✗ | ✗ |
| Content URI triggers | ✓ | ✗ | ✗ | ✗ |

## Retry + backoff

| Feature | Android | iOS | Desktop | Web |
|---------|:-:|:-:|:-:|:-:|
| Exponential backoff | ✓ | ✓ | ✓ | ✓ |
| Linear backoff | ✓ | ✓ | ✓ | ✓ |
| Custom max attempts | ✓ | ✓ | ✓ | ✓ |

## Phase 7 additions (alpha04 + alpha04.X)

| Feature | Android | iOS | Desktop | Web |
|---------|:-:|:-:|:-:|:-:|
| `setExpedited(OutOfQuotaPolicy)` (12+) | 🕒 alpha04.X | ✗ no-op | ✗ no-op | ✗ no-op |
| Foreground service types (14+) | 🕒 alpha04.X | ✗ | ✗ | ✗ |
| `setInitialDelay(Duration)` | 🕒 alpha04.X | 🕒 (earliestBeginDate) | 🕒 (delay before launch) | 🕒 (setTimeout) |
| BGAppRefreshTaskRequest | ✗ | 🕒 alpha04.X | ✗ | ✗ |
| Info.plist contract validation | ✗ | 🕒 alpha04.X (init-time check) | ✗ | ✗ |
| Periodic Background Sync API | ✗ | ✗ | ✗ | 🕒 alpha04.X (Chrome only) |
| Notifications API | n/a (uses Android notifications) | n/a (UNNotifications) | n/a (system tray) | 🕒 alpha04.X (consumer-opt-in) |

## Phase 1 — Foreground tasks (alpha01)

| Feature | Android | iOS | Desktop | Web |
|---------|:-:|:-:|:-:|:-:|
| `ForegroundWorker` base class | ✓ API | ✓ API | ✓ API | ✓ API |
| `setForeground(ForegroundInfo)` | ✓ API; alpha01.X impl | ✓ API; alpha01.X impl | ✓ API; alpha01.X impl | ✓ API; alpha01.X impl |
| `ForegroundServiceType` enum (13 values) | ✓ | n/a | n/a | n/a |

## Phase 8 — Desktop true-background (alpha05)

| Feature | Win | macOS | Linux |
|---------|:-:|:-:|:-:|
| OS scheduler integration | 🕒 (schtasks) | 🕒 (launchd) | 🕒 (systemd-user + cron fallback) |
| JAR integrity SHA-256 check | 🕒 | 🕒 | 🕒 |
| HMAC-signed persistence | 🕒 | 🕒 | 🕒 |

## Phase 9 — Web universal background (alpha06)

| Feature | Chrome | Firefox | Safari 16.4+ | Edge |
|---------|:-:|:-:|:-:|:-:|
| Web Push subscription | 🕒 alpha06 | 🕒 | 🕒 (iOS PWA only) | 🕒 |
| Service Worker push handler | 🕒 | 🕒 | 🕒 | 🕒 |

## See also

- [features/foreground-tasks.md](../features/foreground-tasks.md)
- [true-background-matrix.md](./true-background-matrix.md)
- Phase 7 sub-plan: `plan-layer/.../07-native-api-parity.md`
