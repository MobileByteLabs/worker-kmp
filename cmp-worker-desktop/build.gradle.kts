import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.publish)
    id("io.github.mobilebytelabs.dokka")
}

group = "io.github.mobilebytelabs"
version = providers.gradleProperty("worker.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        jvmMain {
            dependencies {
                api(project(":cmp-worker-kmp"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                // worker-kmp-cmp-launchers-02: launcher API surface
                implementation(compose.desktop.currentOs)
                implementation(compose.runtime)
                implementation(libs.koin.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.koin.test)
            }
        }
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-desktop",
    )
    pom {
        name.set("worker-desktop")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — JVM/Desktop platform module")
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
