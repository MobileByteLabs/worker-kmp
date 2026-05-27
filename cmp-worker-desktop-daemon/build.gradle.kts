import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

// Desktop true-background daemon — JVM-only executable JAR (Phase 8 of worker-kmp v3.0.0 epic).
//
// The daemon is invoked by the host OS scheduler (Windows Task Scheduler / macOS launchd /
// Linux systemd-user timer / cron) to process pending work when the consumer app is not
// running. alpha05 ships the scaffold only (logs + exits); per-OS installer impls and full
// work execution land in v3.0.0-alpha05.X follow-ups.
//
// NOTE: this module uses `kotlin("jvm")` rather than the multiplatform plugin because
// (a) the daemon entry point is a `main(args)` function that only makes sense on the JVM,
// (b) `application` plugin auto-applies `java` which is incompatible with the multiplatform
// plugin. Sibling modules use the multiplatform plugin; this is a second JVM-only exception
// alongside `cmp-worker-bench`.
//
// NOTE: This module produces an executable JAR (the daemon) that the host OS scheduler
// invokes. Full shadowJar fat-JAR packaging lands in alpha05.X follow-up — for alpha05
// we ship the source + tasks scaffold.

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
    api(project(":cmp-worker-kmp"))
    implementation(project(":cmp-worker-desktop"))
    implementation(libs.kermit)
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

application {
    mainClass.set("io.github.mobilebytelabs.worker.daemon.DesktopBackgroundDaemonKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
