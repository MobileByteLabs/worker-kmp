package io.github.mobilebytelabs.worker.convention

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project

internal fun Project.configureSpotless(extension: SpotlessExtension) = extension.apply {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt", "**/.gradle/**/*.kt", "**/samples/**/*.kt")
        ktlint(libs.findVersion("ktlint").get().requiredVersion)
            .editorConfigOverride(
                mapOf(
                    "android" to "true",
                    "indent_size" to "4",
                    "continuation_indent_size" to "4",
                    "max_line_length" to "120",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_package-name" to "disabled",
                    "ktlint_standard_backing-property-naming" to "disabled",
                ),
            )
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**/*.gradle.kts", "**/build-logic/**/*.gradle.kts")
        ktlint(libs.findVersion("ktlint").get().requiredVersion)
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }

    format("misc") {
        target("**/*.md", "**/.gitignore", "**/*.yaml", "**/*.yml")
        targetExclude("**/build/**", "**/.gradle/**")
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}
