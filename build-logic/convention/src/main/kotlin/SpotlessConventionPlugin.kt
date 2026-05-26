import io.github.mobilebytelabs.worker.convention.configureSpotless
import io.github.mobilebytelabs.worker.convention.spotlessGradle
import org.gradle.api.Plugin
import org.gradle.api.Project

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            spotlessGradle { configureSpotless(this) }
        }
    }
}
