import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    // ── JVM Desktop ────────────────────────────────────────────────────────────
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
        mainRun {
            mainClass.set("io.github.mobilebytelabs.worker.sample.composestore.MainKt")
        }
    }

    // ── iOS — Compose Multiplatform framework consumed by iosApp/ Xcode project
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // ── Web (wasmJs) ───────────────────────────────────────────────────────────
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("cmp-worker-sample-compose-store")
        browser {
            commonWebpackConfig { outputFileName = "cmp-worker-sample-compose-store.js" }
        }
        binaries.executable()
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("io.github.mobilebytelabs.worker.ExperimentalWorkerApi")
    }

    sourceSets {
        commonMain {
            dependencies {
                // Single all-in-one bundle replaces the 4 individual worker-kmp deps
                // (worker-kmp + worker-compose + worker-koin + worker-store5) AND the
                // 4 per-platform factories (worker-android/-desktop/-ios/-web) — all
                // re-exported via api(project(...)) in cmp-worker-compose-all.
                implementation(project(":cmp-worker-compose-all"))
                // Annotations are SOURCE-retention markers; the sample exercises them
                // as a documentation/discoverability vehicle. Full plugin codegen lives
                // in cmp-worker-app-plugin and requires consumer-side `apply` wiring
                // (a follow-up to this epic — see PLAN-worker-kmp-app-plugin §7).
                implementation(project(":cmp-worker-app-annotations"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(compose.runtime)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(libs.koin.compose)
            }
        }
        jvmMain {
            dependencies {
                // cmp-worker-desktop already transitively provided by the bundle's
                // desktopMain api re-export — only adding Compose Desktop-specific
                // deps (Window, preview) the bundle doesn't need to ship.
                implementation(compose.desktop.currentOs)
                implementation(compose.preview)
            }
        }
        // androidMain / iosMain / wasmJsMain deps no longer needed — all 4 platform
        // factories + launchers come in transitively via cmp-worker-compose-all.
    }
}
