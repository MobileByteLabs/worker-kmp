# cmp-worker-app-plugin

Gradle plugin that codegens per-platform Compose Multiplatform launcher files
for worker-kmp from a single `@WorkerKmpApp` annotation in commonMain.
**Consumer's source tree has zero per-platform Kotlin files.**

## Setup

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("io.github.mobilebytelabs.worker-app") version "$workerVersion"
}

workerKmpApp {
    androidGenerator = true   // default
    desktopGenerator = true
    iosGenerator = true
    webGenerator = true
}
```

## Usage

In your commonMain:

```kotlin
@WorkerKmpApp(
    title = "My App",
    iosBundleId = "com.example.myapp",
    webCanvasId = "composeCanvas",
)
fun appKoinModules(factory: WorkManagerFactory): List<Module> = listOf(
    appModule(),
    workModule(factory),
)

@WorkerKmpAppContent
@Composable
fun AppContent() = MyRootScreen()
```

That's it. Run:

```bash
./gradlew :myApp:workerKmpAppCodegenAll       # generate all 4 platform launchers
./gradlew :myApp:workerKmpAppXcodegenGenerate # also materialize iosApp.xcodeproj via xcodegen
./gradlew :myApp:assemble                     # full build picks up generated sources
```

The plugin generates:

- `build/generated/worker-kmp-app/androidMain/kotlin/{pkg}/generated/Generated_App.kt`
- `build/generated/worker-kmp-app/androidMain/kotlin/{pkg}/generated/Generated_MainActivity.kt`
- `build/generated/worker-kmp-app/androidMain/AndroidManifest.xml` (manifest-merged with consumer's)
- `build/generated/worker-kmp-app/desktopMain/kotlin/{pkg}/generated/Generated_Main.kt`
- `build/generated/worker-kmp-app/iosMain/kotlin/{pkg}/generated/Generated_MainViewController.kt`
- `build/generated/worker-kmp-app/wasmJsMain/kotlin/{pkg}/generated/Generated_Main.kt`
- `build/generated/worker-kmp-app/wasmJsMain/resources/index.html`
- `iosApp/project.yml` + `iosApp/iosApp/{iOSApp,ContentView}.swift` + `iosApp/iosApp/Info.plist`
  (the `.xcodeproj` is generated from `project.yml` by `xcodegen` — also written here, gitignore the `.xcodeproj/`)

## Opt-out (per platform)

To keep your hand-authored launcher for a specific platform, set the corresponding
generator flag to `false`:

```kotlin
workerKmpApp {
    androidGenerator = false   // skip Android — keep your manually-authored Application
}
```

The plugin also detects pre-existing `Application.kt` / `MainActivity.kt` / `Main.kt` /
`MainViewController.kt` files and skips that source set's codegen with a warning —
zero-config opt-out via file presence.

## How it works

1. Annotations live in `cmp-worker-app-annotations` (KMP, source-retention).
2. `cmp-worker-app-ksp` is a KSP `SymbolProcessor` that finds them in your
   commonMain and writes a `codegen-model.json` to
   `build/generated/ksp/metadata/commonMain/resources/META-INF/worker-kmp-app/`.
3. The plugin's per-platform codegen tasks read that JSON, render Kotlin
   templates, and write the generated launcher files into
   `build/generated/worker-kmp-app/{sourceSet}/...`.
4. The plugin wires those generated dirs into your KMP source sets so the
   regular `compile*Kotlin*` tasks pick them up.

## See also

- `cmp-worker-app-annotations/README.md` — annotation spec
- Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-app-plugin/`
- `samples/cmp-worker-sample-compose-store/` — canonical demo (after Phase 7 migration)
