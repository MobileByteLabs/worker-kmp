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
    // Needed by WorkerComposeConventionPlugin to access KotlinMultiplatformExtension
    // for the worker opt-in.
    compileOnly(libs.kotlin.gradlePlugin)
    // Brings the worker-app plugin class onto this convention plugin's runtime
    // classpath so `pluginManager.apply("io.github.mobilebytelabs.worker-app")`
    // finds the META-INF/gradle-plugins descriptor at runtime. `implementation`
    // (not compileOnly) is required because the descriptor lookup happens when
    // the convention plugin executes, not at its own compile time.
    // Substituted from the cmp-worker-app-plugin includedBuild (build-logic's
    // settings.gradle.kts). External adopters depend on the published artifact
    // from Maven Central — same convention plugin code.
    implementation("io.github.mobilebytelabs:worker-app-plugin:${providers.gradleProperty("worker.version").get()}")
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
        register("workerCompose") {
            id = "io.github.mobilebytelabs.worker.compose-sample"
            implementationClass = "WorkerComposeConventionPlugin"
            description = "All-in-one convention plugin for worker-kmp Compose Multiplatform samples — applies kotlin-multiplatform + compose + worker-app, configures the 5-target KMP matrix + standard sample deps + opt-ins."
        }
    }
}
