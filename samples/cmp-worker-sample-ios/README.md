# cmp-worker-sample-ios

> iOS Compose Multiplatform sample app demonstrating worker-kmp v3 on iOS via
> ComposeUIViewController. Lands at v3.0.0-alpha07.

## Current state (alpha04)

**SCAFFOLD ONLY.** Full Xcode project ships at v3.0.0-alpha07 (per Phase 5 of the
v3.0.0 epic). This directory currently contains:

- This README + the canonical Info.plist additions you'll need

## When fully shipped (alpha07)

Will contain:
- `iosApp.xcodeproj` — Xcode project
- `iosApp/ContentView.swift` — SwiftUI UIViewControllerRepresentable wrapping the
   Kotlin-side `MainViewController()` factory
- `iosApp/Info.plist` — with BGTaskSchedulerPermittedIdentifiers + BGContinuedProcessingTaskRequest IDs
- `Podfile` if CocoaPods; or `Package.swift` for SPM
- Fastlane lane: `bundle exec fastlane ios build_sample`

## Info.plist additions (when integrating worker-kmp on iOS)

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER).processing</string>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER).refresh</string>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER).continued</string>
</array>
<key>UIBackgroundModes</key>
<array>
    <string>processing</string>
    <string>fetch</string>
</array>
```

## Sample scope (when shipped)

Two tabs in the sample's main view:
1. **Scheduler** — schedule one-time + periodic work with constraint pickers
2. **Monitor** — LazyColumn of WorkInfo cards with progress + cancel/retry buttons

Plus a BackgroundCapabilitiesBanner showing iOS-specific capability matrix.
