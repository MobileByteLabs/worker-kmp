import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

// NOTE: This module pulls TWO classpath flavors — v2Compat (worker-kmp 2.1.0 from
// Maven Central) and v3Compat (current local build). The SAME test classes run
// against both classpaths; divergence in test outputs fails CI.
//
// Phase 13 ships the infrastructure; per-deprecation tests for v2.x init wrappers
// (initializeWorkerAndroid / initIosWorkManager / initializeWorkerDesktop /
// initWebWorkManager + PlatformWorkManager.configure global slot) are added in
// Phase 0 when those wrappers become @Deprecated.

val v2Compat by configurations.creating { description = "worker-kmp 2.1.0 from Maven Central — baseline for backward-compat assertions" }
val v3Compat by configurations.creating { description = "Current in-development worker-kmp build" }

dependencies {
    // Common test deps
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // v2Compat: pull released v2.1.0 from Maven Central
    v2Compat("io.github.mobilebytelabs:worker-kmp-jvm:2.1.0")
    v2Compat("io.github.mobilebytelabs:worker-koin-jvm:2.1.0")

    // v3Compat: current local build
    v3Compat(project(":cmp-worker-kmp"))
    v3Compat(project(":cmp-worker-koin"))
}

// Two test tasks: v2BcTest + v3BcTest. Both run the SAME compiled test classes.
tasks {
    val compileTestKotlin by existing(KotlinCompile::class)

    val v2BcTest by registering(Test::class) {
        description = "Runs BC tests against worker-kmp:2.1.0 from Maven Central — establishes baseline"
        group = "verification"
        useJUnitPlatform()
        classpath = sourceSets["test"].output + v2Compat
        testClassesDirs = sourceSets["test"].output.classesDirs
        reports.junitXml.outputLocation.set(layout.buildDirectory.dir("reports/junit/v2-bc-test"))
        dependsOn(compileTestKotlin)
    }

    val v3BcTest by registering(Test::class) {
        description = "Runs BC tests against current local v3 build — must match v2 baseline behavior"
        group = "verification"
        useJUnitPlatform()
        classpath = sourceSets["test"].output + v3Compat
        testClassesDirs = sourceSets["test"].output.classesDirs
        reports.junitXml.outputLocation.set(layout.buildDirectory.dir("reports/junit/v3-bc-test"))
        dependsOn(compileTestKotlin)
    }

    // CI-friendly aggregate
    val bcTestAll by registering {
        group = "verification"
        description = "Runs both v2BcTest and v3BcTest"
        dependsOn(v2BcTest, v3BcTest)
    }

    // checkDeprecatedCoverage — enforces every @Deprecated symbol has a paired BC test.
    // Phase 0 will populate this; for v2.2.0 baseline, no @Deprecated symbols exist yet
    // so the task currently exits 0 trivially.
    val checkDeprecatedCoverage by registering {
        group = "verification"
        description = "Fails if a @Deprecated symbol exists without a paired BC test"
        doLast {
            // Phase 0 populates this with the real scanner. For now, just log:
            logger.lifecycle("checkDeprecatedCoverage: no @Deprecated symbols in v2.2.0 baseline; trivially passing.")
            logger.lifecycle("This task gains real teeth in v3.0.0-alpha00 (Phase 0) when initializeWorkerXxx() become @Deprecated.")
        }
    }
}

// Exclude from BCV (this module produces no public API)
// (assumes parent build.gradle.kts has BCV apiValidation { ignoredProjects.add(...) })
