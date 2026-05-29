# cmp-worker-app-annotations

Annotations module for the `worker-kmp-app-plugin` Gradle plugin. Consumers
reference these from `commonMain`; the plugin codegens per-platform launcher
files at build time.

```kotlin
// commonMain
@WorkerKmpApp(title = "My App", iosBundleId = "com.example.myapp")
fun appKoinModules(factory: WorkManagerFactory): List<Module> = ...

@WorkerKmpAppContent
@Composable
fun AppContent() = MyRootScreen()
```

Applied via the Gradle plugin:

```kotlin
plugins {
    id("io.github.mobilebytelabs.worker-app") version "$workerVersion"
}
```

See `cmp-worker-app-plugin/README.md` for the end-to-end adoption pattern.
