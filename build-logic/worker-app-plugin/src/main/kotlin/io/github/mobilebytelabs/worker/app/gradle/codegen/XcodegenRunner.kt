package io.github.mobilebytelabs.worker.app.gradle.codegen

import java.io.File

/**
 * Runs `xcodegen generate` inside the iosApp dir to materialize iosApp.xcodeproj
 * from the generated project.yml.
 */
internal object XcodegenRunner {

    fun run(iosAppDir: File, xcodegenPath: String): Int = ProcessBuilder(xcodegenPath, "generate")
        .directory(iosAppDir)
        .inheritIO()
        .start()
        .waitFor()
}
