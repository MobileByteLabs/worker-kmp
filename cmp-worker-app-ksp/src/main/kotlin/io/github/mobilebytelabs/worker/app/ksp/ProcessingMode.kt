package io.github.mobilebytelabs.worker.app.ksp

/**
 * Which integration shape a consumer declared, decided purely from the count of each
 * annotation kind discovered in a KSP round. Extracted as a pure function so the
 * decision — including the "Shape 2" workers-only path added for GitHub issue #51 — is
 * unit-testable without a live [com.google.devtools.ksp.processing.Resolver].
 */
internal enum class ProcessingMode {
    /** Nothing annotated yet — no-op round (consumer hasn't applied annotations, or first round). */
    NONE,

    /** Shape 1 — full-app codegen: `@WorkerKmpApp` (+ `@WorkerKmpAppContent`) declared. */
    APP,

    /**
     * Shape 2 — bring-your-own-Application: ONLY `@WorkerKmpWorkers` declared. The plugin
     * skips launcher/app generation and emits only the worker registry + install shim.
     */
    WORKERS_ONLY,
}

internal object ProcessingModeDecider {

    /**
     * Decide the processing mode from annotation counts.
     *
     * - No annotations at all → [ProcessingMode.NONE].
     * - No `@WorkerKmpApp` and no `@WorkerKmpAppContent`, but ≥1 `@WorkerKmpWorkers`
     *   → [ProcessingMode.WORKERS_ONLY] (Shape 2, issue #51).
     * - Anything else (any `@WorkerKmpApp` / `@WorkerKmpAppContent` present) →
     *   [ProcessingMode.APP], where the existing exactly-one-of-each contract is enforced.
     */
    fun decide(appCount: Int, contentCount: Int, workersCount: Int): ProcessingMode = when {
        appCount == 0 && contentCount == 0 && workersCount == 0 -> ProcessingMode.NONE
        appCount == 0 && contentCount == 0 && workersCount > 0 -> ProcessingMode.WORKERS_ONLY
        else -> ProcessingMode.APP
    }
}
