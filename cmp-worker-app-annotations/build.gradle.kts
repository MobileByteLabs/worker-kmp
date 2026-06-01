import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * cmp-worker-app-annotations — KMP annotations module for worker-kmp-app-plugin.
 *
 * Exposes @WorkerKmpApp and @WorkerKmpAppContent annotations consumers reference
 * from commonMain. Source-retention (SOURCE) — annotations consumed at build time
 * by the cmp-worker-app-ksp processor + cmp-worker-app-plugin Gradle plugin;
 * no runtime dependency or reflection.
 *
 * Spec: plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-app-plugin/
 *       01-annotations.md
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
    id("io.github.mobilebytelabs.dokka")
    // NOTE: NOT opting into kover here. cmp-worker-app-annotations contains only
    // @Retention(SOURCE) annotation declarations — zero runtime bytecode — so
    // any coverage report would be 0/0. The Kover.kt root filter already excludes
    // the annotation FQNs defensively. Skipping the convention avoids a Kover
    // 0.9.1 limitation where it can't find the `android` extension on modules
    // that use the new `android.kotlin.multiplatform.library` AGP 9+ plugin.
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    android {
        namespace = "io.github.mobilebytelabs.worker.app.annotations"
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

    sourceSets {
        commonMain {
            // Annotations are stdlib-only; no extra deps.
        }
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-app-annotations",
    )
    pom {
        name.set("worker-app-annotations")
        description.set(
            "WorkManager-equivalent for Kotlin Multiplatform — @WorkerKmpApp + @WorkerKmpAppContent annotations consumed by worker-kmp-app-plugin",
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
