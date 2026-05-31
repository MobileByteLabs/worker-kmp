rootProject.name = "worker-kmp"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        ivy {
            name = "Node.js"
            setUrl("https://nodejs.org/dist")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                ivy("v[revision]/ivy.xml")
            }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy {
            name = "Yarn"
            setUrl("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        // worker-kmp-cmp-launchers-03: wasmJs sample's executable() distribution
        // needs binaryen for the wasm-opt pass. Standard Compose Multiplatform
        // wasmJs setup — pulls binaryen from GitHub releases.
        ivy {
            name = "Binaryen"
            setUrl("https://github.com/WebAssembly/binaryen/releases/download")
            patternLayout {
                artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

include(":cmp-worker-kmp")
include(":cmp-worker-desktop")
include(":cmp-worker-android")
include(":cmp-worker-compose")
include(":cmp-worker-compose-all")
include(":cmp-worker-app-annotations")
include(":cmp-worker-app-ksp")
// :cmp-worker-app-plugin moved to build-logic/worker-app-plugin/ per kmp-product-flavors
// pattern. Published via the dedicated module-path publish workflow job.
include(":cmp-worker-web")
include(":cmp-worker-test")
include(":cmp-worker-ios")
include(":cmp-worker-koin")
include(":cmp-worker-scheduler")
include(":cmp-worker-store5")
include(":cmp-worker-storeflow")
include(":samples:cmp-worker-sample")
include(":samples:cmp-worker-sample-android")
include(":samples:cmp-worker-sample-compose-store")
include(":cmp-worker-bench")
include(":cmp-worker-desktop-daemon")
include(":cmp-worker-web-push")
