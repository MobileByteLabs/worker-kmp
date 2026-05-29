# Convention Plugin (build-logic)

If your project uses a `build-logic` composite build to share Gradle convention plugins across modules, you can wrap worker-kmp's library wiring into a single convention plugin instead of repeating the same `plugins { … }` + `dependencies { … }` block in every module that schedules work.

The canonical implementation ships in this repo at [`build-logic/convention/src/main/kotlin/WorkerComposeConventionPlugin.kt`](https://github.com/MobileByteLabs/worker-kmp/blob/development/build-logic/convention/src/main/kotlin/WorkerComposeConventionPlugin.kt). It's intentionally short — single-concern, copy-and-adopt — and is the pattern used by the canonical [`samples/cmp-worker-sample-compose-store`](https://github.com/MobileByteLabs/worker-kmp/tree/development/samples/cmp-worker-sample-compose-store) sample.

## When to use it

Use a convention plugin if **any** of these apply:

- You ship multiple commonMain modules that each schedule work
- You already maintain a `build-logic` composite build for KMP/Compose/Detekt/Spotless conventions
- You want one place to bump worker-kmp's version across the project
- Your team treats "the way we wire a library in" as a documented convention, not per-module copy-paste

Don't bother if you have a single CMP module — applying worker-kmp directly in that module's `build.gradle.kts` (per the [Quick Start](quick-start.md)) is simpler.

## What the convention plugin does

Three things — nothing else:

1. Applies the `io.github.mobilebytelabs.worker-app` Gradle plugin (which eliminates per-platform launcher Kotlin via KSP-driven codegen)
2. Adds the `cmp-worker-compose-all` bundle + `koin-compose` to `commonMain`
3. Enables the `io.github.mobilebytelabs.worker.ExperimentalWorkerApi` opt-in

It does **not** decide your KMP target matrix, your Compose Multiplatform version, your dep BOM, or your platform-specific extras. Those stay in each consumer module — the convention plugin is library wiring only, not an app-framework starter kit.

## Setup

### 1. Add to `libs.versions.toml`

```toml
[versions]
worker-version = "2.1.2"

[libraries]
worker-compose-all = { module = "io.github.mobilebytelabs:worker-compose-all", version.ref = "worker-version" }
koin-compose      = { module = "io.insert-koin:koin-compose",                 version = "4.0.4" }
kotlin-gradlePlugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version = "2.3.21" }

[plugins]
worker-app = { id = "io.github.mobilebytelabs.worker-app", version.ref = "worker-version" }
```

### 2. Declare the Gradle plugin (build-logic)

```kotlin
// build-logic/convention/build.gradle.kts
plugins { `kotlin-dsl` }

dependencies {
    // Brings the worker-app plugin class onto this convention plugin's RUNTIME
    // classpath so pluginManager.apply("io.github.mobilebytelabs.worker-app")
    // resolves the META-INF/gradle-plugins descriptor at runtime.
    // (NOT compileOnly — the lookup happens when the convention plugin executes.)
    implementation("io.github.mobilebytelabs:worker-app-plugin:${providers.gradleProperty("worker.version").get()}")

    // For KotlinMultiplatformExtension access (compileOnly is fine here).
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("workerCompose") {
            id = "your.org.worker.compose"
            implementationClass = "WorkerComposeConventionPlugin"
        }
    }
}
```

```properties
# build-logic/gradle.properties  (included builds don't inherit parent gradle.properties)
worker.version=2.1.2
```

### 3. The convention plugin

```kotlin
// build-logic/convention/src/main/kotlin/WorkerComposeConventionPlugin.kt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class WorkerComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            pluginManager.apply("io.github.mobilebytelabs.worker-app")

            dependencies {
                add("commonMainImplementation", libs.findLibrary("worker-compose-all").get())
                add("commonMainImplementation", libs.findLibrary("koin-compose").get())
            }

            extensions.configure<KotlinMultiplatformExtension> {
                compilerOptions {
                    optIn.add("io.github.mobilebytelabs.worker.ExperimentalWorkerApi")
                }
            }
        }
    }
}
```

### 4. Apply in consumer module

```kotlin
// any consumer module's build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("your.org.worker.compose")
}

kotlin {
    // your KMP targets — jvm/iosArm64/iosSimulatorArm64/wasmJs as needed.
    // The convention plugin doesn't dictate these.
    jvm {
        mainRun {
            // The worker-app plugin codegens this entry — package matches the file
            // containing your @WorkerKmpApp function.
            mainClass.set("com.example.app.generated.Generated_MainKt")
        }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "ComposeApp"; isStatic = true } }
    wasmJs { browser(); binaries.executable() }
}
```

That's it — your `commonMain/` annotates `@WorkerKmpApp` + `@WorkerKmpAppContent` and the worker-app plugin codegens every per-platform launcher into `build/generated/worker-kmp-app/`.

## A note on `pluginManager.apply` from inside a convention plugin

The convention plugin uses `pluginManager.apply("io.github.mobilebytelabs.worker-app")` rather than the `plugins {}` block — because at convention-plugin-execution time you can't open a new `plugins {}` scope on the target project. For this programmatic apply to succeed, Gradle needs the plugin's `META-INF/gradle-plugins/<id>.properties` descriptor on the convention plugin's **runtime** classpath. That's why the build-logic dep above uses `implementation(...)` not `compileOnly(...)` — `compileOnly` keeps the descriptor off the runtime classpath and the plugin resolution fails with `Plugin with id 'io.github.mobilebytelabs.worker-app' not found.`

## What you gain

- One source of truth for the worker-kmp version, dep bundle, and opt-in across every module
- Adding worker-kmp to a new module = adding one line to its `plugins {}` block
- Bumping worker-kmp version = bumping `worker-version` in `libs.versions.toml` once
- The convention plugin file is small (~25 lines including the imports) and self-contained — code review is fast

## Mirror in the worker-kmp repo

The same convention plugin lives in this repo for our own samples. There it has two extra Gradle gymnastics:

- `build-logic/settings.gradle.kts` does `includeBuild("../cmp-worker-app-plugin")` so the in-development plugin module is on the convention plugin's classpath without a `publishToMavenLocal` round-trip
- Root `settings.gradle.kts` adds a `dependencySubstitution` rule mapping `io.github.mobilebytelabs:worker-compose-all` → `:cmp-worker-compose-all` because the in-monorepo project name differs from the published artifact id

**Neither of these is needed in an external project.** When you pull worker-kmp from Maven Central, both the plugin and the bundle resolve normally — your `build-logic/convention/build.gradle.kts` declares the dep with version, and that's the whole story.

## See also

- [Quick Start](quick-start.md) — simpler one-file setup for single-module projects
- [Installation](installation.md) — dep matrix + version requirements
- [`samples/cmp-worker-sample-compose-store`](https://github.com/MobileByteLabs/worker-kmp/tree/development/samples/cmp-worker-sample-compose-store) — canonical working example using this exact convention plugin
