package io.github.mobilebytelabs.worker.app.gradle

import java.util.Properties

/**
 * Resolves the worker-kmp version used to wire the `worker-app-annotations` +
 * `worker-app-ksp` Maven coordinates for EXTERNAL consumers (GitHub issue #51, bug 1).
 *
 * Root cause of the reported `Could not find …:worker-app-ksp:unspecified`: the plugin
 * used to fall back to `project.version` when `worker.version` was unset — but at
 * `apply()` time `project` is the *consumer's* project, whose version is Gradle's literal
 * `"unspecified"` for a normal app. The `version "X"` in the consumer's
 * `plugins { id(...) version "X" }` block only selects the plugin-marker artifact; it is
 * invisible to the applied plugin's code.
 *
 * The plugin now bakes its OWN version into a classpath resource at plugin-build time (see
 * `build.gradle.kts` → `generateWorkerAppVersionResource`) and reads it here. Extracted as a
 * pure object so the precedence + fallback rules are unit-testable without a Gradle build.
 */
internal object WorkerVersionResolver {

    /** Classpath location of the version resource baked in at plugin-build time. */
    const val VERSION_RESOURCE = "/worker-app-version.properties"

    /** Gradle's placeholder for an unset project version — never a valid coordinate version. */
    const val UNSPECIFIED = "unspecified"

    /**
     * Resolve the coordinate version.
     *
     * Precedence:
     *  1. [propertyOverride] — an explicit non-blank `worker.version` gradle property.
     *  2. [embedded] — the plugin's own baked-in version (ignored when blank or "unspecified").
     *
     * Deliberately NEVER considers the consumer's `project.version`. Throws with an
     * actionable message when neither source yields a usable version.
     */
    fun resolve(propertyOverride: String?, embedded: String?): String {
        propertyOverride?.takeIf { it.isNotBlank() }?.let { return it }
        embedded?.takeIf { it.isNotBlank() && it != UNSPECIFIED }?.let { return it }
        error(
            "io.github.mobilebytelabs.worker-app: could not resolve the worker-kmp version " +
                "needed to wire the annotations + KSP processor dependencies. The plugin jar is " +
                "missing its embedded version resource ($VERSION_RESOURCE). As a workaround, set " +
                "`worker.version=<the plugin version>` in your gradle.properties.",
        )
    }

    /**
     * Reads the plugin's own published version from the baked-in classpath resource.
     * Returns null when the resource is absent or its `version` entry is missing.
     */
    fun loadEmbedded(): String? =
        WorkerVersionResolver::class.java.getResourceAsStream(VERSION_RESOURCE)?.use { stream ->
            Properties().apply { load(stream) }.getProperty("version")
        }
}
