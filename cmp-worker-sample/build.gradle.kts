import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
            mainClass.set("io.github.mobilebytelabs.worker.sample.SampleAppKt")
        }
    }
    js(IR) {
        nodejs()
        binaries.executable()
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("io.github.mobilebytelabs.worker.ExperimentalWorkerApi")
    }

    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":cmp-worker-desktop"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        jsMain {
            dependencies {
                implementation(project(":cmp-worker-web"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
