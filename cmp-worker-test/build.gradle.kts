import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.vanniktech.publish)
    id("io.github.mobilebytelabs.kover")
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
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser()
        nodejs()
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":cmp-worker-kmp"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

mavenPublishing {
    signAllPublications()
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-test",
    )
    pom {
        name.set("worker-test")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — test utilities for consumers")
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
