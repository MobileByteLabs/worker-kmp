# iosApp — Xcode wrapper for `cmp-worker-sample-compose-store`

This directory ships an [xcodegen](https://github.com/yonaskolb/XcodeGen) `project.yml`
declarative spec instead of a committed `iosApp.xcodeproj`. Rationale lives in the
`project.yml` comment block (merge hostility of UUID-keyed pbxproj).

## Generating the Xcode project

```bash
brew install xcodegen   # one-time
cd samples/cmp-worker-sample-compose-store/iosApp
xcodegen generate
# → samples/cmp-worker-sample-compose-store/iosApp/iosApp.xcodeproj appears
```

`iosApp.xcodeproj/` is gitignored — regenerate any time the `project.yml` changes
or after a fresh checkout.

## Running

```bash
# From the source repo root:
./gradlew :samples:cmp-worker-sample-compose-store:linkDebugFrameworkIosSimulatorArm64
cd samples/cmp-worker-sample-compose-store/iosApp
xcodegen generate
xcodebuild \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -destination 'generic/platform=iOS Simulator' \
    -configuration Debug \
    build
```

Then open `iosApp.xcodeproj` in Xcode + Cmd-R to launch the simulator interactively.

## Shape

- `iosApp/iOSApp.swift` — SwiftUI `@main` declaration.
- `iosApp/ContentView.swift` — bridges SwiftUI to Kotlin/Native via
  `UIViewControllerRepresentable` wrapping `MainViewControllerKt.MainViewController()`.
- `iosApp/Info.plist` — standard iOS app manifest.

The Kotlin side that this Swift wraps is at
`../src/iosMain/kotlin/io/github/mobilebytelabs/worker/sample/composestore/MainViewController.kt`
— a 3-line `fun MainViewController(): UIViewController` calling
`workerKmpMainViewController(...)` from `cmp-worker-ios`.
