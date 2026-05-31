import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * cmp-worker-compose-all — all-in-one Compose Multiplatform worker library.
 *
 * Single dep for CMP consumer apps. Re-exports via `api(project(...))`:
 *  - cmp-worker-kmp          (core API)
 *  - cmp-worker-compose      (Compose UI helpers — WorkInfoCard, etc.)
 *  - cmp-worker-koin              (Koin DI integration)
 *  - cmp-worker-scheduler         (high-level WorkScheduler + Syncable contracts)
 *  - cmp-worker-scheduler-store5  (Store5 adapter — StoreSyncable + MutableStoreSyncable)
 *  - cmp-worker-store5            (StoreBackedWorker)
 *  - cmp-worker-android           (androidMain — Android factory + launchers)
 *  - cmp-worker-desktop      (jvmMain/desktopMain — Desktop factory + launcher)
 *  - cmp-worker-ios          (iosMain — iOS factory + launcher)
 *  - cmp-worker-web          (jsMain + wasmJsMain — Web factory + launcher)
 *
 * The 4 platform-specific modules continue to publish independently — consumers
 * who want granular deps (e.g. pure-Android no-Compose) keep their fine-grained
 * options. This bundle is the OPT-IN simpler-deps path for CMP apps.
 *
 * Spec: plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-compose-all-bundle/GOAL.md
 */

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
        namespace = "io.github.mobilebytelabs.worker.compose.all"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        withHostTestBuilder {}.configure {}
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser {
            testTask { enabled = false }
        }
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask { enabled = false }
        }
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":cmp-worker-kmp"))
                api(project(":cmp-worker-compose"))
                api(project(":cmp-worker-koin"))
                api(project(":cmp-worker-scheduler"))
                api(project(":cmp-worker-scheduler-store5"))
                api(project(":cmp-worker-store5"))
            }
        }
        androidMain {
            dependencies {
                api(project(":cmp-worker-android"))
            }
        }
        val desktopMain by getting {
            dependencies {
                api(project(":cmp-worker-desktop"))
            }
        }
        iosMain {
            dependencies {
                api(project(":cmp-worker-ios"))
            }
        }
        jsMain {
            dependencies {
                api(project(":cmp-worker-web"))
            }
        }
        val wasmJsMain by getting {
            dependencies {
                api(project(":cmp-worker-web"))
            }
        }
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-compose-all",
    )
    pom {
        name.set("worker-compose-all")
        description.set(
            "WorkManager-equivalent for Kotlin Multiplatform — all-in-one Compose Multiplatform bundle (core + UI + Koin + Store5 + 4 platforms)",
        )
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
