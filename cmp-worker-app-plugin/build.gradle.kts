/*
 * cmp-worker-app-plugin — Gradle plugin entry for worker-kmp-app-plugin epic.
 *
 * Applied to consumer projects via:
 *   plugins {
 *       id("io.github.mobilebytelabs.worker-app") version "$workerVersion"
 *   }
 *
 * Pulls in KSP + the annotations module + the KSP processor, registers per-target
 * codegen tasks, wires generated source dirs into the consumer's KMP source sets.
 *
 * Spec: plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-app-plugin/
 *       03-gradle-plugin-skeleton.md  (skeleton)
 *       04-android-codegen.md          (Android Application + Activity + manifest)
 *       05-desktop-web-codegen.md      (jvm Main.kt + wasmJs Main.kt + index.html)
 *       06-ios-codegen.md              (MainViewController + xcodegen project.yml + Swift)
 */

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${libs.versions.kotlin.get()}")

    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test.junit)
}

gradlePlugin {
    website.set("https://github.com/MobileByteLabs/worker-kmp")
    vcsUrl.set("https://github.com/MobileByteLabs/worker-kmp.git")
    plugins {
        register("workerApp") {
            id = "io.github.mobilebytelabs.worker-app"
            implementationClass = "io.github.mobilebytelabs.worker.app.gradle.WorkerKmpAppPlugin"
            displayName = "worker-kmp app plugin"
            description =
                "Codegens per-platform Compose Multiplatform launcher files for worker-kmp from @WorkerKmpApp annotations in commonMain — eliminates per-platform launcher Kotlin entirely."
            tags.set(listOf("kotlin", "multiplatform", "compose", "compose-multiplatform", "worker-kmp", "ksp"))
        }
    }
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "skipped", "failed") }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-app-plugin",
    )
    pom {
        name.set("worker-app-plugin")
        description.set(
            "WorkManager-equivalent for Kotlin Multiplatform — Gradle plugin that codegens per-platform Compose Multiplatform launcher files from @WorkerKmpApp commonMain annotations",
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
