# iOS Swift Integration Guide

This guide shows how to integrate `worker-kmp` into a SwiftUI or UIKit iOS app.

## 1. Add the XCFramework

In your `build.gradle.kts` for the shared KMP module:

```kotlin
kotlin {
    iosArm64()
    iosSimulatorArm64()
    // ...
    sourceSets {
        iosMain.dependencies {
            implementation("io.github.mobilebytelabs:worker-ios:<version>")
        }
    }
}
```

The shared module exposes `initIosWorkManager` and `PlatformWorkManager` to Swift via the generated Objective-C header.

## 2. Application Entry Point

### SwiftUI App

```swift
import SwiftUI
import shared // your KMP shared module

@main
struct MyApp: App {
    init() {
        setupWorkerKmp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

private func setupWorkerKmp() {
    InitIosWorkManagerKt.initIosWorkManager(
        workerFactory: MyWorkerFactory(),
        config: .companion.default
    )
}
```

### UIKit AppDelegate

```swift
import UIKit
import shared

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        InitIosWorkManagerKt.initIosWorkManager(
            workerFactory: MyWorkerFactory()
        )
        return true
    }
}
```

## 3. Worker Factory

Implement `IosWorkerFactory` to resolve workers by class name:

```swift
import shared

class MyWorkerFactory: IosWorkerFactory {
    func create(workerClass: String, context: any WorkerContext) throws -> any CoroutineWorker {
        switch workerClass {
        case "SyncWorker":
            return SyncWorker(context: context)
        case "ImageWorker":
            return ImageWorker(context: context)
        default:
            throw NSError(
                domain: "MyWorkerFactory",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Unknown worker: \(workerClass)"]
            )
        }
    }
}
```

## 4. Define a Worker

Workers are defined in your shared Kotlin module and automatically compiled for iOS:

```kotlin
// shared/src/iosMain/kotlin/SyncWorker.kt
class SyncWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val url = inputData.getString("url") ?: return WorkResult.failure("missing url")
        // perform sync ...
        return WorkResult.success(workDataOf("bytes" to 1024L))
    }
}
```

## 5. Enqueue Work from Swift

```swift
import shared

// Get WorkManager from PlatformWorkManager
let wm = PlatformWorkManager()

// Build a one-time request
let request = OneTimeWorkRequestBuilder<SyncWorker>()
    .setInputData(WorkDataKt.workDataOf(["url": "https://example.com/data"]))
    .addTag("sync")
    .build()

// Enqueue (Kotlin suspend → Swift async via async wrapper)
Task {
    do {
        let id = try await wm.enqueue(request: request)
        print("Enqueued work: \(id)")
    } catch {
        print("Enqueue failed: \(error)")
    }
}
```

## 6. Observe Work Status

```swift
import Combine
import shared

// Collect a Kotlin Flow as a Swift AsyncSequence via Kotlin-Swift interop
func observeWork(tag: String, workManager: WorkManager) {
    let flow = workManager.getWorkInfosByTag(tag: tag)
    Task {
        for try await infos in flow {
            for info in infos {
                print("Work \(info.id): \(info.state)")
            }
        }
    }
}
```

## 7. Background Scheduling (opt-in)

Enable `BGTaskScheduler` by providing a task identifier:

```kotlin
// In shared module — configure iOS WorkManager with BGTask identifier
@OptIn(ExperimentalWorkerApi::class)
fun initIos(factory: IosWorkerFactory) {
    initIosWorkManager(
        workerFactory = factory,
        config = IosWorkManagerConfig(
            bgTaskIdentifier = "com.myapp.background-sync",
        )
    )
}
```

In your `Info.plist`, declare the background task:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>com.myapp.background-sync</string>
</array>
```

## 8. Koin DI (optional)

If your shared module uses Koin:

```kotlin
// Shared Koin module
val appModule = module {
    single { MyRepository(get()) }
    // workKoinModule already provides single<WorkManager>
}

fun initShared() {
    startKoin {
        modules(workKoinModule, appModule)
    }
}
```

Call `initShared()` from `AppDelegate.application(_:didFinishLaunchingWithOptions:)` **before** `initIosWorkManager`, so that `PlatformWorkManager.configure()` is called first and Koin resolves it correctly.
