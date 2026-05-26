plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.mobilebytelabs"
version = libs.versions.worker.get()

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":worker-kmp"))
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
    coordinates(
        groupId = "io.github.mobilebytelabs",
        artifactId = "worker-web",
        version = libs.versions.worker.get()
    )
    pom {
        name.set("worker-web")
        description.set("WorkManager-equivalent for Kotlin Multiplatform — Web (JS/Wasm) platform module")
        url.set("https://github.com/mobilebytelabs/worker-kmp")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
    }
}
