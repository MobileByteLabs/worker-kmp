package io.github.mobilebytelabs.worker.android

import android.app.Application
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Structural test for [WorkerKmpStarterApplication].
 *
 * Application lifecycle (onCreate, Koin start, androidContext wiring) requires Robolectric or
 * instrumented tests; covered separately. This unit test asserts the class shape so a
 * refactor that breaks the public-API contract of "abstract Application subclass requiring
 * a koinModules() override" fails the build.
 *
 * Spec: GOAL.md AC4 + AC10. Idempotent Koin start is exercised by integration tests in the
 * `cmp-worker-sample-compose-store` sample (Phase 3).
 */
class WorkerKmpStarterApplicationTest {

    @Test
    fun `extends Application (open subclass contract)`() {
        val cls = WorkerKmpStarterApplication::class.java
        var current: Class<*>? = cls.superclass
        var foundApplication = false
        while (current != null) {
            if (current.name == Application::class.java.name) {
                foundApplication = true
                break
            }
            current = current.superclass
        }
        assertTrue(foundApplication, "WorkerKmpStarterApplication must extend android.app.Application")
    }

    @Test
    fun `is abstract — consumers must override koinModules()`() {
        val cls = WorkerKmpStarterApplication::class.java
        assertTrue(
            java.lang.reflect.Modifier.isAbstract(cls.modifiers),
            "WorkerKmpStarterApplication must be abstract so consumers are forced to declare koinModules()",
        )
    }

    @Test
    fun `declares koinModules() abstract method`() {
        val method = requireNotNull(
            WorkerKmpStarterApplication::class.java.declaredMethods
                .firstOrNull { it.name == "koinModules" && it.parameterCount == 0 },
        ) { "WorkerKmpStarterApplication must declare koinModules(): List<Module>" }
        assertTrue(
            java.lang.reflect.Modifier.isAbstract(method.modifiers),
            "koinModules() must be abstract — Koin start depends on consumer-provided modules",
        )
    }
}
