package io.github.mobilebytelabs.worker.convention

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

internal fun Project.configureDetekt(extension: DetektExtension) {
    extension.apply {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        baseline = file("$rootDir/config/detekt/baseline.xml")
        parallel = true
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        reports {
            html.required.set(true)
            xml.required.set(true)
            txt.required.set(false)
            sarif.required.set(false)
        }
    }

    dependencies.add("detektPlugins", libs.findLibrary("detekt-formatting").get())
}
