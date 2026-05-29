// cmp-worker-app-plugin lives in a sibling included build (alongside this one in
// the root settings.gradle.kts pluginManagement). Including it here too lets the
// convention plugin take a compile-time dep on the worker-app plugin class +
// programmatically apply it by ID. External adopters depend on the published
// `io.github.mobilebytelabs:worker-app-plugin` artifact from Maven Central
// instead — same convention plugin code, just a different resolution path.
//
// Substitution rule needed because the included build's rootProject.name is
// "cmp-worker-app-plugin" but the published artifact id is "worker-app-plugin".
includeBuild("../cmp-worker-app-plugin") {
    dependencySubstitution {
        substitute(module("io.github.mobilebytelabs:worker-app-plugin")).using(project(":"))
    }
}

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

