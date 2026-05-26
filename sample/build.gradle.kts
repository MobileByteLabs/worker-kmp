import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "io.github.mobilebytelabs"
version = "1.0.0"

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

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":worker-desktop"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
