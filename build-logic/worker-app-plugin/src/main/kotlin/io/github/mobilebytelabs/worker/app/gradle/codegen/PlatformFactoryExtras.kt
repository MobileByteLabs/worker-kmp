package io.github.mobilebytelabs.worker.app.gradle.codegen

/**
 * Builds the `{{platformFactoryImport}}` + `{{platformFactoryCall}}` template extras
 * based on whether the consumer's `@WorkerKmpApp` function takes a `WorkManagerFactory`.
 *
 * When [takesFactory] is true: import the platform factory + emit a `xxxWorkManagerFactory()`
 * call into the codegen-rendered launcher. When false: both extras resolve to the empty string,
 * so the rendered call becomes `koinFn()` (the v4.0.0 no-arg shorthand).
 */
internal fun factoryExtras(
    takesFactory: Boolean,
    factoryFqn: String,
    factoryCall: String,
): Map<String, String> = mapOf(
    "platformFactoryImport" to if (takesFactory) "import $factoryFqn\n" else "",
    "platformFactoryCall" to if (takesFactory) factoryCall else "",
)
