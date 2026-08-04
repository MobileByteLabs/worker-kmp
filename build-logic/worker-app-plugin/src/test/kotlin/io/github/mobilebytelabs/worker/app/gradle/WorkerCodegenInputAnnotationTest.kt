package io.github.mobilebytelabs.worker.app.gradle

import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression guard for the Gradle 8+/9 implicit-dependency validation failure.
 *
 * `sourceSetDirs` is populated from the consumer source set's `srcDirs`, which — for a
 * Compose Multiplatform consumer — INCLUDE the compose resource-generator output dirs
 * (`build/generated/compose/resourceGenerator/kotlin/<ss>ResourceAccessors` and
 * `…<ss>ResourceCollectors`, produced by `generateResourceAccessorsFor<SS>` /
 * `generateActualResourceCollectorsFor<SS>`). Declaring those as `@InputFiles` without a
 * producer dependency makes Gradle fail every `workerKmpAppCodegen*` task with
 *   "Task ':…:workerKmpAppCodegen<P>' uses this output of task
 *    ':…:generateResourceAccessorsFor<SS>Main' without declaring an … dependency."
 *
 * `sourceSetDirs` feeds only [PreexistingLauncherDetector], a best-effort *warning* scan, and
 * these tasks are always-run (`@Internal generatedRoot`, no declared outputs) so input tracking
 * buys no incrementality. It MUST therefore be `@Internal`, not an input — that removes the
 * property from Gradle's input-validation set while staying configuration-cache safe (the
 * FileCollection is captured + serialized at configuration time).
 *
 * Reflection-based (Gradle task annotations are `@Retention(RUNTIME)`) so it is fast and needs
 * no Compose-resource GradleRunner fixture to reproduce the failure.
 */
class WorkerCodegenInputAnnotationTest {

    private fun getter(name: String) = AbstractWorkerCodegenTask::class.java.getDeclaredMethod(name)

    @Test
    fun `sourceSetDirs is @Internal not @InputFiles (no implicit dependency on compose-generated dirs)`() {
        val g = getter("getSourceSetDirs")
        assertTrue(
            g.isAnnotationPresent(Internal::class.java),
            "sourceSetDirs must be @Internal so Gradle does not validate it as an input that " +
                "implicitly depends on the compose resource-generator output dirs",
        )
        assertFalse(
            g.isAnnotationPresent(InputFiles::class.java),
            "sourceSetDirs must NOT be @InputFiles — it captures build/generated compose-resource " +
                "dirs without a producer dependency, which fails Gradle 9 implicit-dependency validation",
        )
    }

    @Test
    fun `codegenModel stays an input (its producer dependency is declared)`() {
        // Guard against an over-broad fix: codegenModel legitimately stays @InputFiles because its
        // producer (kspCommonMainKotlinMetadata) IS wired via dependsOn.
        val g = getter("getCodegenModel")
        assertTrue(
            g.isAnnotationPresent(InputFiles::class.java),
            "codegenModel must remain @InputFiles (KSP model is a real, dependency-declared input)",
        )
    }
}
