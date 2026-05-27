import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.2"
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

// JVM-only benchmark module — NOT published.
// Phase 11 (PLAN worker-kmp-v3-foreground-storeflow-11-performance-benchmarks).
// JMH 1.37 micro-benchmarks for hot paths surfaced through v2.1.0.
//
// Run a full benchmark suite: ./gradlew :cmp-worker-bench:jmh           (~15 min)
// Quick mode (CI regression):   ./gradlew :cmp-worker-bench:jmh \
//                                     -Pjmh.iterations=2 \
//                                     -Pjmh.warmupIterations=2 \
//                                     -Pjmh.fork=1                       (~3 min)
//
// NOTE: this module uses `kotlin("jvm")` rather than the multiplatform plugin because
// (a) JMH only runs on the JVM, (b) the JMH Gradle plugin auto-applies `java` which
// is incompatible with `kotlin.multiplatform`. Sibling modules use the multiplatform
// plugin; this is the lone JVM-only exception.

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":cmp-worker-kmp"))
    implementation(project(":cmp-worker-test"))
    implementation(libs.kotlinx.coroutines.core)

    // JMH source set — added by the me.champeau.jmh plugin.
    "jmh"(libs.kotlinx.coroutines.core)
    "jmh"(project(":cmp-worker-kmp"))
    "jmh"(project(":cmp-worker-test"))
    "jmh"("org.openjdk.jmh:jmh-core:1.37")
    "jmhAnnotationProcessor"("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    jmhVersion.set("1.37")
    resultFormat.set(providers.gradleProperty("jmh.resultFormat").orElse("TEXT"))
    resultsFile.set(
        layout.file(
            providers.gradleProperty("jmh.resultsFile").map { file(it) }
                .orElse(layout.buildDirectory.file("results/jmh/results.txt").map { it.asFile }),
        ),
    )
    val iterProp = providers.gradleProperty("jmh.iterations").map { it.toInt() }
    val warmupProp = providers.gradleProperty("jmh.warmupIterations").map { it.toInt() }
    val forkProp = providers.gradleProperty("jmh.fork").map { it.toInt() }
    if (iterProp.isPresent) iterations.set(iterProp)
    if (warmupProp.isPresent) warmupIterations.set(warmupProp)
    if (forkProp.isPresent) fork.set(forkProp)
}
