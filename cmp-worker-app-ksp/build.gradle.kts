/*
 * cmp-worker-app-ksp — KSP SymbolProcessor for worker-kmp-app-plugin.
 *
 * JVM-only module (KSP processors run on JVM regardless of consumer target).
 * Scans the consumer's commonMain for @WorkerKmpApp + @WorkerKmpAppContent
 * annotations, validates the function shapes, and emits a serialized
 * CodegenModel to a build-dir JSON file that the cmp-worker-app-plugin
 * Gradle plugin reads.
 *
 * Spec: plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-app-plugin/
 *       02-ksp-processor.md
 */

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.serialization)
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
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinx.serialization.json)

    // NOTE: kotlin-compile-testing-ksp (kctfork) functional tests for KSP processors
    // are intentionally deferred: kctfork 0.5.0 ships its own kotlin-compiler-embeddable
    // 2.0.0 which can't read symbol-processing-api 2.3.x metadata. The processor is
    // exercised end-to-end via the cmp-worker-app-plugin Gradle plugin integration
    // (apply plugin → consumer build → codegen-model.json materializes).
    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "skipped", "failed") }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-app-ksp",
    )
    pom {
        name.set("worker-app-ksp")
        description.set(
            "WorkManager-equivalent for Kotlin Multiplatform — KSP SymbolProcessor that emits CodegenModel JSON consumed by worker-kmp-app-plugin",
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
