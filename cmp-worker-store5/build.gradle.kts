import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.vanniktech.publish)
    id("io.github.mobilebytelabs.dokka")
    id("io.github.mobilebytelabs.kover")
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

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
                // Scheduler is api-scoped because StoreSyncable / MutableStoreSyncable
                // adapters extend Syncable from cmp-worker-scheduler; consumers using
                // these adapters need that contract on their classpath.
                api(project(":cmp-worker-scheduler"))
                api(libs.store5)
                // koin-core is api-scoped because `workStore5KoinModule` returns a koin `Module`
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
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test.junit)
            }
        }
    }
}

// Store5 5.1.0-alpha06 was built against kotlinx-datetime 0.6.x where
// `kotlinx.datetime.Clock.System` lived in this package. In 0.8.0 (current
// project version) Clock moved to `kotlin.time` stdlib, so Store5's compiled
// bytecode references break at JVM-test runtime with NoClassDefFoundError.
// Force the jvmTest runtime classpath back to a binary-compatible version of
// kotlinx-datetime so `StoreWriteRequest.of(...)` (used by
// MutableStoreSyncWorker.doWork) doesn't fail at test time. This does NOT
// affect main / publish classpath (we still publish against 0.8.0).
configurations.matching { it.name.startsWith("jvmTest") }.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.2")
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-store5",
    )
    pom {
        name.set("worker-store5")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — Mobile Native Foundation Store5 bridge")
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
