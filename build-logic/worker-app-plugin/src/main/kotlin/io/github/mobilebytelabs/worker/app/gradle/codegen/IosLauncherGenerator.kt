package io.github.mobilebytelabs.worker.app.gradle.codegen

import io.github.mobilebytelabs.worker.app.gradle.CodegenModel
import io.github.mobilebytelabs.worker.app.gradle.TemplateEngine
import java.io.File

/**
 * iOS codegen — emits 5 files:
 *   Kotlin side (into `kotlinOutputDir`):
 *     - `kotlin/{pkg}/generated/MainViewController.kt`
 *   iOS-app side (into `iosAppDir` — usually `samples/.../iosApp` or `iosApp/`):
 *     - `project.yml`                      (xcodegen spec)
 *     - `iosApp/iOSApp.swift`              (SwiftUI @main)
 *     - `iosApp/ContentView.swift`         (UIViewControllerRepresentable bridge)
 *     - `iosApp/Info.plist`                (iOS app manifest)
 *
 * The .xcodeproj itself is materialized by the separate XcodegenRunner step.
 *
 * Spec: 06-ios-codegen.md
 */
internal object IosLauncherGenerator {

    fun run(model: CodegenModel, kotlinOutputDir: File, iosAppDir: File) {
        val pkgPath = model.packageName.replace('.', '/')
        val extras = factoryExtras(
            takesFactory = model.koinModulesFnTakesFactory,
            factoryFqn = "io.github.mobilebytelabs.worker.ios.iosWorkManagerFactory",
            factoryCall = "iosWorkManagerFactory()",
        )

        kotlinOutputDir.resolve("kotlin/$pkgPath/generated/MainViewController.kt").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("ios-mainviewcontroller.kt.template"), model, extras))
        }
        iosAppDir.resolve("project.yml").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("ios-project.yml.template"), model))
        }
        iosAppDir.resolve("iosApp/iOSApp.swift").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("ios-iosApp.swift.template"), model))
        }
        iosAppDir.resolve("iosApp/ContentView.swift").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("ios-ContentView.swift.template"), model))
        }
        iosAppDir.resolve("iosApp/Info.plist").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("ios-Info.plist.template"), model))
        }
    }
}
