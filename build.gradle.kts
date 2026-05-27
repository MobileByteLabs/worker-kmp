plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.dokka)
    id("io.github.mobilebytelabs.spotless")
    id("io.github.mobilebytelabs.dokka")
    id("io.github.mobilebytelabs.detekt")
}

allprojects {
    group = "io.github.mobilebytelabs"
}

// BCV — only track public API modules (not sample or test helpers)
apiValidation {
    ignoredProjects += listOf("cmp-worker-sample", "cmp-worker-sample-android", "cmp-worker-test")
    nonPublicMarkers += listOf("io.github.mobilebytelabs.worker.ExperimentalWorkerApi")
}

tasks.named("check") {
    dependsOn("detekt", "spotlessCheck")
}

tasks.register("fix") {
    group = "verification"
    description = "Applies all automatic fixes including Spotless formatting"
    dependsOn("spotlessApply")
}
