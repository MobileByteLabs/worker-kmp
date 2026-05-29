/*
 * cmp-worker-app-plugin standalone Gradle build.
 *
 * Lifted into its own build (not a regular `include(":...")` in the root) so
 * that the main build's `pluginManagement { includeBuild("cmp-worker-app-plugin") }`
 * makes the `io.github.mobilebytelabs.worker-app` plugin available via the
 * plugins DSL to other modules in the main build (e.g. samples/).
 *
 * Reuses the parent's version catalog so dep versions stay aligned.
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "cmp-worker-app-plugin"
