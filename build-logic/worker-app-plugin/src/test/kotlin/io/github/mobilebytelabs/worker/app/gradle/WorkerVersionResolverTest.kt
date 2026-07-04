package io.github.mobilebytelabs.worker.app.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for the version-resolution fix (GitHub issue #51, bug 1):
 * `Could not find io.github.mobilebytelabs:worker-app-ksp:unspecified`.
 */
class WorkerVersionResolverTest {

    @Test
    fun `explicit worker_version override is used`() {
        assertEquals("4.0.0", WorkerVersionResolver.resolve(propertyOverride = "4.0.0", embedded = null))
    }

    @Test
    fun `override wins over embedded version`() {
        assertEquals("9.9.9", WorkerVersionResolver.resolve(propertyOverride = "9.9.9", embedded = "4.0.0"))
    }

    @Test
    fun `falls back to the baked-in plugin version when no override`() {
        assertEquals("4.0.0", WorkerVersionResolver.resolve(propertyOverride = null, embedded = "4.0.0"))
    }

    @Test
    fun `blank override is ignored and embedded is used`() {
        assertEquals("4.0.0", WorkerVersionResolver.resolve(propertyOverride = "   ", embedded = "4.0.0"))
    }

    @Test
    fun `never resolves to the consumer's unspecified project version`() {
        // This is the exact reported failure: a consumer app with no version set produced
        // `worker-app-ksp:unspecified`. The resolver must refuse "unspecified" outright.
        val ex = assertFailsWith<IllegalStateException> {
            WorkerVersionResolver.resolve(propertyOverride = null, embedded = "unspecified")
        }
        assertTrue("worker.version" in (ex.message ?: ""), "error should point at the workaround")
    }

    @Test
    fun `throws with an actionable message when nothing is resolvable`() {
        val ex = assertFailsWith<IllegalStateException> {
            WorkerVersionResolver.resolve(propertyOverride = null, embedded = null)
        }
        assertTrue("worker.version" in (ex.message ?: ""))
    }

    @Test
    fun `plugin jar carries an embedded version resource that is not unspecified`() {
        // End-to-end guard: the build task `generateWorkerAppVersionResource` must have
        // baked worker-app-version.properties onto the classpath. Without it, external
        // consumers regress to the `:unspecified` failure.
        val embedded = WorkerVersionResolver.loadEmbedded()
        assertTrue(
            embedded != null && embedded.isNotBlank() && embedded != WorkerVersionResolver.UNSPECIFIED,
            "embedded plugin version must be present + concrete, was: $embedded",
        )
    }
}
