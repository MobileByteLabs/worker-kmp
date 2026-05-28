// Minimal reference worker-kmp Web Push server (Ktor variant).
//
// Added in worker-kmp v3.0.0-alpha06.X (Phase 9 alpha06.X). Standalone — NOT included
// in the root `settings.gradle.kts`. To run from this directory:
//
//   ./gradlew run
//
// Set the VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY env vars before launching.

plugins {
    kotlin("jvm") version "2.1.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "3.0.3"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("nl.martijndwars:web-push:5.1.1")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}
