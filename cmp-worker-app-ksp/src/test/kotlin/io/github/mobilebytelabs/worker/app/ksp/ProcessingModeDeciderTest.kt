package io.github.mobilebytelabs.worker.app.ksp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the integration-shape decision (GitHub issue #51, bug 2). The
 * "Shape 2" bring-your-own-Application path — only `@WorkerKmpWorkers`, no
 * `@WorkerKmpApp`/`@WorkerKmpAppContent` — must be recognised instead of erroring
 * with "expected exactly one @WorkerKmpApp-annotated function; found 0".
 */
class ProcessingModeDeciderTest {

    @Test
    fun `no annotations is a no-op round`() {
        assertEquals(
            ProcessingMode.NONE,
            ProcessingModeDecider.decide(appCount = 0, contentCount = 0, workersCount = 0),
        )
    }

    @Test
    fun `only WorkerKmpWorkers is the workers-only shape`() {
        assertEquals(
            ProcessingMode.WORKERS_ONLY,
            ProcessingModeDecider.decide(appCount = 0, contentCount = 0, workersCount = 1),
        )
    }

    @Test
    fun `multiple WorkerKmpWorkers sites are still workers-only`() {
        assertEquals(
            ProcessingMode.WORKERS_ONLY,
            ProcessingModeDecider.decide(appCount = 0, contentCount = 0, workersCount = 3),
        )
    }

    @Test
    fun `WorkerKmpApp present is the app shape`() {
        assertEquals(
            ProcessingMode.APP,
            ProcessingModeDecider.decide(appCount = 1, contentCount = 1, workersCount = 1),
        )
    }

    @Test
    fun `app without workers is the app shape`() {
        assertEquals(
            ProcessingMode.APP,
            ProcessingModeDecider.decide(appCount = 1, contentCount = 1, workersCount = 0),
        )
    }

    @Test
    fun `content annotation without app still routes to app shape (so the exactly-one check fires)`() {
        // @WorkerKmpAppContent present but no @WorkerKmpApp is an INCOMPLETE app declaration,
        // NOT workers-only — it must reach the app path so the existing validation errors.
        assertEquals(
            ProcessingMode.APP,
            ProcessingModeDecider.decide(appCount = 0, contentCount = 1, workersCount = 1),
        )
    }
}
