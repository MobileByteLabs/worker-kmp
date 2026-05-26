import io.github.mobilebytelabs.worker.convention.configureDetekt
import io.github.mobilebytelabs.worker.convention.detektGradle
import org.gradle.api.Plugin
import org.gradle.api.Project

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")
            detektGradle { configureDetekt(this) }
        }
    }
}
