package io.github.mobilebytelabs.worker.app.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Consumer-facing DSL block:
 *
 * ```kotlin
 * workerKmpApp {
 *     androidGenerator = true   // default — set false to skip Android codegen
 *     desktopGenerator = true
 *     iosGenerator = true
 *     webGenerator = true
 *     xcodegenPath = "xcodegen"   // override if not on PATH
 *     wasmJsBundleName = "myAppBundle"   // optional — defaults to project.name
 * }
 * ```
 */
public abstract class WorkerKmpAppExtension {
    public abstract val androidGenerator: Property<Boolean>
    public abstract val desktopGenerator: Property<Boolean>
    public abstract val iosGenerator: Property<Boolean>
    public abstract val webGenerator: Property<Boolean>
    public abstract val xcodegenPath: Property<String>
    public abstract val wasmJsBundleName: Property<String>
    public abstract val androidExtraManifestEntries: ListProperty<String>
}
