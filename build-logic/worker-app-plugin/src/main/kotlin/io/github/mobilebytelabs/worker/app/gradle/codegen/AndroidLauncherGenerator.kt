package io.github.mobilebytelabs.worker.app.gradle.codegen

import io.github.mobilebytelabs.worker.app.gradle.CodegenModel
import io.github.mobilebytelabs.worker.app.gradle.TemplateEngine
import java.io.File

/**
 * Android codegen — emits 3 files into `outputDir`:
 *   - `kotlin/{pkg}/generated/Generated_App.kt`
 *   - `kotlin/{pkg}/generated/Generated_MainActivity.kt`
 *   - `AndroidManifest.xml`     (merged with consumer's via AGP)
 *
 * Spec: 04-android-codegen.md
 */
internal object AndroidLauncherGenerator {

    fun run(model: CodegenModel, outputDir: File) {
        val pkgPath = model.packageName.replace('.', '/')
        // Android special-case — the factory call is `androidWorkManagerFactory(this)` because
        // we're inside the generated Application subclass.
        val androidExtras = mapOf(
            "platformFactoryImport" to if (model.koinModulesFnTakesFactory) {
                "import io.github.mobilebytelabs.worker.android.androidWorkManagerFactory\n"
            } else {
                ""
            },
            "androidFactoryCall" to if (model.koinModulesFnTakesFactory) "androidWorkManagerFactory(this)" else "",
        )

        outputDir.resolve("kotlin/$pkgPath/generated/Generated_App.kt").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("android-app.kt.template"), model, androidExtras))
        }
        outputDir.resolve("kotlin/$pkgPath/generated/Generated_MainActivity.kt").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("android-activity.kt.template"), model))
        }
        outputDir.resolve("AndroidManifest.xml").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("AndroidManifest.xml.template"), model))
        }
    }
}
