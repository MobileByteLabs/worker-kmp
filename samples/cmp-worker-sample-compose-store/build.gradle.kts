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
                implementation(project(":cmp-worker-kmp"))
                implementation(project(":cmp-worker-koin"))
                implementation(project(":cmp-worker-store5"))
                implementation(project(":cmp-worker-compose"))
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
                implementation(project(":cmp-worker-desktop"))
                implementation(compose.desktop.currentOs)
                implementation(compose.preview)
            }
        }
        iosMain {
            dependencies {
                implementation(project(":cmp-worker-ios"))
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":cmp-worker-web"))
            }
        }
    }
}
