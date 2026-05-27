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
    // Phase 10 (security threat model) — CycloneDX SBOM generation for supply-chain transparency.
    // Applied at root to aggregate a full-project SBOM via the `cyclonedxBom` task.
    // CI consumed by .github/workflows/security-scan.yml (sbom-cyclonedx job).
    id("org.cyclonedx.bom") version "1.10.0"
}

// Phase 10 — CycloneDX SBOM output configuration.
// Emits build/reports/bom.json which the security-scan workflow uploads as artifact.
tasks.named("cyclonedxBom") {
    // Defaults are correct: JSON format at build/reports/bom.json.
    // Future tightening (alpha04+): pin schemaVersion, restrict includeConfigs.
}

allprojects {
    group = "io.github.mobilebytelabs"
}

// BCV — only track public API modules (not sample or test helpers)
apiValidation {
    ignoredProjects += listOf("cmp-worker-sample", "cmp-worker-sample-android", "cmp-worker-test", "cmp-worker-bc-test", "cmp-worker-desktop-daemon")
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
