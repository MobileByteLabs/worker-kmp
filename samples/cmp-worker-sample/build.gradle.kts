import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        mainRun {
            mainClass.set("io.github.mobilebytelabs.worker.sample.ComposeSampleAppKt")
        }
    }
    js(IR) {
        nodejs()
        binaries.executable()
    }
    wasmJs {
        browser()
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("io.github.mobilebytelabs.worker.ExperimentalWorkerApi")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.ui)
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":cmp-worker-desktop"))
                implementation(project(":cmp-worker-compose"))
                implementation(project(":cmp-worker-koin"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(compose.desktop.currentOs)
                implementation(compose.preview)
            }
        }
        jsMain {
            dependencies {
                implementation(project(":cmp-worker-web"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":cmp-worker-kmp"))
                implementation(project(":cmp-worker-web"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
