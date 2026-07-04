/*
 * worker-app-plugin — Gradle plugin entry for the worker-kmp-app-plugin epic.
 *
 * Lives under build-logic/worker-app-plugin/ (kmp-product-flavors pattern).
 * Consumed via:
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

import java.util.Properties

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.mobilebytelabs"
// Read worker.version from the ROOT build's gradle.properties (single source of
// truth). build-logic is an included composite; Gradle doesn't propagate
// properties across the includedBuild boundary, so we read the file directly.
// rootDir here is the build-logic directory; its parent is the main worker-kmp
// repo root where gradle.properties lives.
version = Properties()
    .apply {
        rootDir.parentFile
            .resolve("gradle.properties")
            .reader()
            .use(::load)
    }.getProperty("worker.version") ?: error(
        "worker.version not found in ${rootDir.parentFile}/gradle.properties",
    )

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

// ── Self-versioning (GitHub issue #51, bug 1) ────────────────────────────────
// Bake the plugin's OWN published version into a resource on its classpath so
// `apply()` can self-identify the coordinate version for the annotations + KSP
// processor deps. Without this, external consumers had to duplicate the version
// as `worker.version=X` in gradle.properties, because the `version "X"` in their
// `plugins { id(...) version "X" }` block is NOT visible to the plugin's apply()
// code — it only selects the plugin-marker artifact. The plugin build already
// resolved its version above (from the root gradle.properties, single SoT); we
// persist it here and read it back via the classloader at apply() time. The
// `worker.version` gradle property remains an OPTIONAL override.
val workerAppVersionResourceDir = layout.buildDirectory.dir("generated/worker-app-version")
val generateWorkerAppVersionResource = tasks.register("generateWorkerAppVersionResource") {
    val versionValue = version.toString()
    val outDir = workerAppVersionResourceDir
    inputs.property("version", versionValue)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("worker-app-version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=$versionValue\n")
    }
}
// Pass the TaskProvider (not the plain dir) to srcDir so Gradle wires an implicit
// task dependency for EVERY consumer of main resources — processResources AND the
// sourcesJar/publishing tasks. Passing only the directory + a processResources
// dependsOn left sourcesJar/publish without a declared dependency, which Gradle 9.5
// rejects as an `implicit_dependency` validation problem at publish time (issue #51
// follow-up: broke `enableAutomaticMavenCentralPublishing` in the 4.0.1 publish).
sourceSets.named("main") {
    resources.srcDir(generateWorkerAppVersionResource)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${libs.versions.kotlin.get()}")
    // KSP plugin marker — placing the marker on this plugin's runtime classpath
    // lets `plugins.apply("com.google.devtools.ksp")` succeed in any consumer
    // without requiring the consumer's pluginManagement to know about KSP.
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${libs.versions.ksp.get()}")

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
    // Explicit Central Portal config — vanniktech's auto-detection skips when
    // com.gradle.plugin-publish is also applied (it assumes plugin-publish will
    // handle Maven Central too, but it doesn't). Without this call the
    // `publishAllPublicationsToMavenCentralRepository` task isn't created and the
    // publish workflow fails with "task not found". `automaticRelease = true`
    // mirrors the project-wide SONATYPE_AUTOMATIC_RELEASE setting.
    publishToMavenCentral(automaticRelease = true)
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
