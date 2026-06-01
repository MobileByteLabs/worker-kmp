import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.github.mobilebytelabs.worker.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.spotless.gradle)
    compileOnly(libs.dokka.gradle)
    compileOnly(libs.kover.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("detekt") {
            id = "io.github.mobilebytelabs.detekt"
            implementationClass = "DetektConventionPlugin"
            description = "Configures detekt static analysis for worker-kmp modules"
        }
        register("spotless") {
            id = "io.github.mobilebytelabs.spotless"
            implementationClass = "SpotlessConventionPlugin"
            description = "Configures spotless code formatting for worker-kmp modules"
        }
        register("dokka") {
            id = "io.github.mobilebytelabs.dokka"
            implementationClass = "DokkaConventionPlugin"
            description = "Configures Dokka HTML documentation for worker-kmp modules"
        }
        register("kover") {
            id = "io.github.mobilebytelabs.kover"
            implementationClass = "KoverConventionPlugin"
            description = "Configures Kover code coverage for worker-kmp modules — self-registers into root's aggregation"
        }
    }
}
