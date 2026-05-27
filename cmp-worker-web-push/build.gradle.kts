import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.vanniktech.publish)
    id("io.github.mobilebytelabs.dokka")
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

// Web Push universal-browser background module (Phase 9 of worker-kmp v3.0.0 epic).
//
// Adds Web Push subscription + Service Worker push handler so that the consumer's
// server can wake the worker on a schedule that survives tab close. alpha06 ships
// the scaffold (public API + Koin module + per-platform stub actuals + SW template
// + smoke tests). Real per-platform impls (JS navigator.serviceWorker + pushManager,
// WasmJs @JsFun bindings, IndexedDB read + work dispatch from SW context,
// BroadcastChannel cross-tab dedup, reference push servers, VAPID Gradle task)
// land in v3.0.0-alpha06.X follow-ups.

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
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
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":cmp-worker-kmp"))
                // koin-core is api-scoped because `workWebPushKoinModule` returns a koin `Module`
                // type in its public API surface — consumers need it on their classpath at use site.
                api(libs.koin.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.kermit)
            }
        }
        iosMain {
            dependencies {
                implementation(libs.kermit)
            }
        }
        // cmp-worker-web targets jvm + js + wasmJs only (no iOS); we depend on it
        // only from the platforms that already use it.
        jsMain {
            dependencies {
                api(project(":cmp-worker-web"))
                implementation(libs.kermit)
            }
        }
        wasmJsMain {
            dependencies {
                api(project(":cmp-worker-web"))
                implementation(libs.kermit)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test.junit)
            }
        }
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-web-push",
    )
    pom {
        name.set("worker-web-push")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — Web Push universal-browser background module")
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
