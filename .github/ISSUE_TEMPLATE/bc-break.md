---
name: BC Break Report
about: Report a backward-compatibility regression between v2.x and current v3
title: "[bc-break] "
labels: bc-break, priority/critical
assignees: ''
---

## Backward-compatibility break

**Affected v2.x API:**
<e.g. `initializeWorkerAndroid(context, factory)` — class, method, deprecation note>

**Affected v3.x behavior:**
<e.g. current v3 build throws `NoSuchMethodError` / different return value / etc.>

## Reproduction

```kotlin
// Consumer code (v2.x style) that previously worked:
initializeWorkerAndroid(context)
val wm = PlatformWorkManager()  // expected: AndroidWorkManager instance
```

**v2.1.0 result (baseline):** <paste>

**Current v3 result (regression):** <paste>

## Environment

- worker-kmp version: <e.g. v3.0.0-alpha01>
- Kotlin: <e.g. 2.3.21>
- Gradle: <e.g. 9.5.1>
- OS: <e.g. macOS 14 / Ubuntu 22.04>

## Workaround (if known)

<paste>
