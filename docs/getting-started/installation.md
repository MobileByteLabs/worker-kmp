---
title: "Installation"
description: "Install worker-kmp from Maven Central — Gradle catalog snippet + per-platform setup for Android, iOS, Desktop, and Web."
---

# Installation

!!! info "Latest version"
    [![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/worker-kmp.svg?label=worker-kmp)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/worker-kmp)
    Replace `LATEST` in the snippets below with the version shown on the badge.

worker-kmp targets Kotlin Multiplatform projects with Android, iOS, Desktop, and Web targets. Compose Multiplatform is supported but optional.

## Requirements

- Kotlin 2.0+
- Gradle 8.5+
- Android: minSdk 21+
- iOS: 13.0+
- Desktop (JVM): 11+
- Web: any modern browser; Service Worker required for true-background

## Add the dependency

### Using version catalog (recommended)

```toml
# gradle/libs.versions.toml
[versions]
worker-kmp = "LATEST"  # ← see the badge above for the current published version

[libraries]
worker-kmp = { module = "io.github.mobilebytelabs:worker-kmp", version.ref = "worker-kmp" }
worker-koin = { module = "io.github.mobilebytelabs:worker-koin", version.ref = "worker-kmp" }
worker-compose = { module = "io.github.mobilebytelabs:worker-compose", version.ref = "worker-kmp" }
worker-android = { module = "io.github.mobilebytelabs:worker-android", version.ref = "worker-kmp" }
worker-ios = { module = "io.github.mobilebytelabs:worker-ios", version.ref = "worker-kmp" }
worker-desktop = { module = "io.github.mobilebytelabs:worker-desktop", version.ref = "worker-kmp" }
worker-web = { module = "io.github.mobilebytelabs:worker-web", version.ref = "worker-kmp" }
```

### commonMain build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.worker.kmp)
            api(libs.worker.koin)
            implementation(libs.worker.compose)  // Compose Multiplatform UI (optional)
        }
        androidMain.dependencies { api(libs.worker.android) }
        iosMain.dependencies { api(libs.worker.ios) }
        jvmMain.dependencies { api(libs.worker.desktop) }
        jsMain.dependencies { api(libs.worker.web) }
        wasmJsMain.dependencies { api(libs.worker.web) }
    }
}
```

## Per-platform setup

After adding deps, see the platform-specific page for any Manifest / Info.plist / etc.:

- [Android](../platform-support/android.md)
- [iOS](../platform-support/ios.md)
- [Desktop](../platform-support/desktop.md)
- [Web](../platform-support/web.md)
