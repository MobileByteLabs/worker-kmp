import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.mobilebytelabs"
version = libs.versions.worker.get()

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        jvmMain {
            dependencies {
                api(project(":worker-kmp"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-desktop",
        version = libs.versions.worker.get()
    )
    pom {
        name.set("worker-desktop")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — JVM/Desktop platform module")
        url.set("https://github.com/mobilebytelabs/worker-kmp")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
    }
}
