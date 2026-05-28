package io.github.mobilebytelabs.worker.android

import androidx.activity.ComponentActivity
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Structural test for [WorkerKmpComposeActivity].
 *
 * Activity lifecycle invocation (instantiation, onCreate, setContent integration) is covered
 * by Robolectric/instrumented tests that would live under `androidInstrumentedTest/`. This
 * unit test asserts the class shape so a refactor that breaks the public-API contract of
 * "open ComponentActivity subclass accepting a content lambda" fails the build.
 *
 * Spec: GOAL.md AC3 + AC10.
 */
class WorkerKmpComposeActivityTest {

    @Test
    fun `extends ComponentActivity (open subclass contract)`() {
        // Existence + open-class contract via reflective superclass check.
        val cls = WorkerKmpComposeActivity::class.java
        var current: Class<*>? = cls.superclass
        var foundComponentActivity = false
        while (current != null) {
            if (current.name == ComponentActivity::class.java.name) {
                foundComponentActivity = true
                break
            }
            current = current.superclass
        }
        assertTrue(foundComponentActivity, "WorkerKmpComposeActivity must extend ComponentActivity")
    }

    @Test
    fun `is open for subclassing`() {
        val cls = WorkerKmpComposeActivity::class.java
        assertTrue(
            !java.lang.reflect.Modifier.isFinal(cls.modifiers),
            "WorkerKmpComposeActivity must be open so consumers can subclass it",
        )
    }

    @Test
    fun `declares a one-arg constructor for the content lambda`() {
        val declared = WorkerKmpComposeActivity::class.java.declaredConstructors
        // The class must declare a constructor accepting one parameter (the content
        // lambda). The exact parameter type can vary across Compose compiler plugin
        // versions (Function0 vs. Function2 with Composer injection), so we
        // intentionally only assert the arity — the parameter type is verified at
        // compile time by `class MainActivity : WorkerKmpComposeActivity({ SampleApp() })`
        // in any consumer.
        val oneArgConstructors = declared.filter { it.parameterCount == 1 }
        assertTrue(
            oneArgConstructors.isNotEmpty(),
            "WorkerKmpComposeActivity must declare a one-arg constructor for the content lambda",
        )
    }
}
