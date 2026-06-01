import io.github.mobilebytelabs.worker.convention.configureKoverRootReports
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Self-registering kover convention plugin.
 *
 * - Applied to the root project (via `alias(libs.plugins.kover.convention)` in
 *   the root plugins block): applies kover and delegates the report-filter +
 *   verify-rule configuration to `Project.configureKoverRootReports()` in
 *   `io.github.mobilebytelabs.worker.convention.Kover.kt` — same shape as
 *   DetektConventionPlugin / SpotlessConventionPlugin delegate to
 *   `detektGradle` / `spotlessGradle`.
 *
 * - Applied to any leaf module: applies kover AND self-registers into root's
 *   aggregation via `rootProject.dependencies.add("kover", project)`. Each
 *   module opts itself in — there is no central `subprojects { }` filter to
 *   maintain. Adding a new cmp-worker-* module to coverage requires zero
 *   changes here: just apply `id("io.github.mobilebytelabs.kover")` in the
 *   module's plugins block.
 */
class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            if (project == rootProject) {
                configureKoverRootReports()
            } else {
                rootProject.dependencies.add("kover", project)
            }
        }
    }
}
