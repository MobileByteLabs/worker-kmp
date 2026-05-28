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
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        mainRun {
            mainClass.set("io.github.mobilebytelabs.worker.sample.composestore.MainKt")
        }
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
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":cmp-worker-desktop"))
                implementation(compose.desktop.currentOs)
                implementation(compose.preview)
            }
        }
    }
}
