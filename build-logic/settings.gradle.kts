dependencyResolutionManagement {
    repositories {
        mavenLocal()
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

rootProject.name = "build-logic"
include(":convention")
// The worker-app Gradle plugin (the published library artifact). Sub-project of
// build-logic so the convention plugin can apply it programmatically (sibling
// classpath) AND the publish workflow can target it via module-path.
include(":worker-app-plugin")

