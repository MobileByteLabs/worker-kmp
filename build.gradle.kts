plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    id("io.github.mobilebytelabs.spotless")
    id("io.github.mobilebytelabs.detekt")
}

allprojects {
    group = "io.github.mobilebytelabs"
}

tasks.named("check") {
    dependsOn("detekt", "spotlessCheck")
}

tasks.register("fix") {
    group = "verification"
    description = "Applies all automatic fixes including Spotless formatting"
    dependsOn("spotlessApply")
}
