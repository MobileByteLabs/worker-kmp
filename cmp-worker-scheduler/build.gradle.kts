import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
    id("io.github.mobilebytelabs.dokka")
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    android {
        namespace = "io.github.mobilebytelabs.worker.scheduler"
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
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    applyDefaultHierarchyTemplate()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser()
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
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
                api(project(":cmp-worker-kmp"))
                implementation(project(":cmp-worker-koin"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.koin.core)
            }
        }
        androidMain {
            dependencies {
                api(project(":cmp-worker-android"))
                // Pulls in androidx.core transitively for NotificationCompat / NotificationManagerCompat
                implementation(libs.androidx.work.runtime.ktx)
            }
        }
        val iosMain by getting {
            dependencies {
                api(project(":cmp-worker-ios"))
            }
        }
        jvmMain {
            dependencies {
                api(project(":cmp-worker-desktop"))
            }
        }
        wasmJsMain {
            dependencies {
                api(project(":cmp-worker-web"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.koin.test)
            }
        }
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-scheduler",
    )
    pom {
        name.set("worker-scheduler")
        description.set(
            "WorkManager-equivalent for Kotlin Multiplatform — high-level WorkScheduler façade with " +
                "daily/periodic/exact-time scheduling + AlarmManager-backed Android exact-tier + " +
                "BGTaskScheduler-backed iOS + Synchronizer/Syncable contracts (NiA-shaped) + " +
                "AbstractDataSyncWorker base class.",
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
