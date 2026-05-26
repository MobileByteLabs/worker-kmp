import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    android {
        namespace = "io.github.mobilebytelabs.worker.android"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
    sourceSets {
        androidMain {
            dependencies {
                api(project(":cmp-worker-kmp"))
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.work.runtime.ktx)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
            }
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-android",
        version = providers.gradleProperty("worker.version").get(),
    )
    pom {
        name.set("worker-android")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — Android platform module")
        url.set("https://github.com/MobileByteLabs/worker-kmp")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
    }
}
