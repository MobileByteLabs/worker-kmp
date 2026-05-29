import io.github.mobilebytelabs.worker.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin that wires the worker-kmp library into a Compose Multiplatform
 * module. Applies the `io.github.mobilebytelabs.worker-app` Gradle plugin (which
 * eliminates per-platform launcher Kotlin via KSP-driven codegen), adds the
 * all-in-one `cmp-worker-compose-all` bundle + `koin-compose` to `commonMain`,
 * and enables the `ExperimentalWorkerApi` opt-in.
 *
 * Pre-conditions (consumer responsibility):
 *  - `org.jetbrains.kotlin.multiplatform` must be applied before this plugin
 *  - the consumer must declare its own KMP target matrix + Compose plugin/deps
 *
 * Reference/inspiration: kmp-project-template's `KMPRoomConventionPlugin` —
 * single-concern library wiring, no opinions on the consumer's KMP/Compose setup.
 *
 * Usage:
 *   plugins {
 *       alias(libs.plugins.kotlin.multiplatform)
 *       alias(libs.plugins.compose.multiplatform)
 *       alias(libs.plugins.kotlin.compose)
 *       id("io.github.mobilebytelabs.worker.compose-sample")
 *   }
 */
class WorkerComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.mobilebytelabs.worker-app")

            dependencies {
                // In-monorepo: prefer project ref to skip a publish round-trip.
                // External adopters: drop the `findProject` check and use
                //   add("commonMainImplementation", libs.findLibrary("worker-compose-all").get())
                // after adding `worker-compose-all = { module = "io.github.mobilebytelabs:worker-compose-all", version = ... }`
                // to their libs.versions.toml.
                val workerComposeAll: Any = rootProject.findProject(":cmp-worker-compose-all")
                    ?: libs.findLibrary("worker-compose-all").get()
                add("commonMainImplementation", workerComposeAll)
                add("commonMainImplementation", libs.findLibrary("koin-compose").get())
            }

            extensions.configure<KotlinMultiplatformExtension> {
                compilerOptions {
                    optIn.add("io.github.mobilebytelabs.worker.ExperimentalWorkerApi")
                }
            }
        }
    }
}
