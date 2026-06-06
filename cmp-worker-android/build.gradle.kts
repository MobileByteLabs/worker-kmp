import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.publish)
    id("io.github.mobilebytelabs.dokka")
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
        // Enable host-side unit tests so androidUnitTest sources run via JVM.
        // Mirrors cmp-worker-compose's setup. Activates :testHost task.
        withHostTestBuilder {}.configure {}
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
                // worker-kmp-cmp-launchers-02: launcher API surface
                implementation(libs.compose.runtime)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
                implementation(libs.koin.core)
                implementation(libs.koin.test)
                implementation(libs.robolectric)
                implementation(libs.androidx.work.testing)
                implementation(libs.androidx.test.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-android",
    )
    pom {
        name.set("worker-android")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — Android platform module")
        url.set("https://github.com/MobileByteLabs/worker-kmp")
        inceptionYear.set("2026")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("MobileByteLabs")
                name.set("MobileByteLabs")
                url.set("https://github.com/MobileByteLabs")
            }
        }
        scm {
            url.set("https://github.com/MobileByteLabs/worker-kmp/")
            connection.set("scm:git:git://github.com/MobileByteLabs/worker-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/MobileByteLabs/worker-kmp.git")
        }
    }
}
