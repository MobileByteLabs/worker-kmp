package io.github.mobilebytelabs.worker.app.gradle

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Configuration-cache compatibility guard for the worker-app codegen tasks.
 *
 * Every codegen task (`workerKmpAppCodegen{Android,Desktop,Ios,Web}`,
 * `workerKmpAppCodegenAutoShim`, `workerKmpAppXcodegenGenerate`) is auto-wired into the
 * consumer's `compile*` chain. If ANY of them calls `notCompatibleWithConfigurationCache(...)`
 * (or captures `Project`/the extension at execution time), the WHOLE build's configuration
 * cache is discarded on every build — Gradle prints
 *   "Configuration cache entry discarded because incompatible task ... was found".
 *
 * This test applies the plugin to a minimal KMP consumer and runs a codegen task with
 * `--configuration-cache` TWICE. A CC-compatible task set REUSES the cache on the 2nd run.
 *
 * KSP is disabled in the fixture so we don't need the (unpublished-in-test) annotations +
 * processor artifacts — we only exercise the codegen tasks' configuration-cache behavior,
 * which is decided at task-registration time, independent of whether a model exists.
 */
class ConfigCacheCompatTest {

    private lateinit var projectDir: File

    private fun write(rel: String, content: String) {
        val f = File(projectDir, rel)
        f.parentFile.mkdirs()
        f.writeText(content.trimIndent() + "\n")
    }

    private fun setUpFixture() {
        projectDir = Files.createTempDirectory("worker-cc-compat").toFile()

        write(
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "cc-compat-consumer"
            """,
        )

        write(
            "gradle.properties",
            """
            org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
            kotlin.code.style=official
            """,
        )

        write(
            "build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform") version "2.3.21"
                id("io.github.mobilebytelabs.worker-app")
            }

            kotlin {
                jvm()
            }

            // A jvm()-only KMP consumer does NOT create the commonMain-metadata KSP task that
            // the codegen tasks `dependsOn`. Register a placeholder so the dependency resolves
            // without a real KSP run (which would need the unpublished processor artifact).
            if (tasks.findByName("kspCommonMainKotlinMetadata") == null) {
                tasks.register("kspCommonMainKotlinMetadata")
            }

            // Keep KSP OUT of the execution graph — the fixture has no annotations and the
            // annotations/processor artifacts aren't published for the test version. Disabling
            // (not excluding by exact name) is robust to KSP's task-naming across versions.
            tasks.matching { it.name.startsWith("ksp") }.configureEach { enabled = false }
            """,
        )
    }

    private fun pluginClasspath(): List<File> {
        fun prop(name: String): List<File> =
            (System.getProperty(name) ?: "")
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .map(::File)
        // worker-app plugin (+ its runtime deps: kotlin-gradle-plugin-api, KSP marker,
        // serialization) UNIFIED with the full kotlin-multiplatform + KSP Gradle plugins so
        // the injected consumer build applies them from ONE classloader (KSP's
        // KotlinBasePluginWrapper ref resolves against the full plugin).
        return (prop("worker.plugin.classpath") + prop("kmp.plugin.classpath")).distinct()
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath(pluginClasspath())
            .withArguments(*args)
            .forwardOutput()

    @AfterTest
    fun tearDown() {
        if (::projectDir.isInitialized) projectDir.deleteRecursively()
    }

    @Test
    fun `codegen task reuses the configuration cache on a second run`() {
        setUpFixture()
        val task = "workerKmpAppCodegenAutoShim"

        // Run 1 — primes the configuration cache (or discards it if a task is incompatible).
        val first = runner(task, "--configuration-cache", "--stacktrace").build()

        // Run 2 — must REUSE the cache. A `notCompatibleWithConfigurationCache` task poisons it.
        val second = runner(task, "--configuration-cache", "--stacktrace").build()
        val out = second.output

        assertFalse(
            out.contains("Configuration cache entry discarded") ||
                out.contains("discarded because incompatible task") ||
                out.contains("cannot serialize object of type 'org.gradle.api.internal.project.DefaultProject'"),
            "configuration cache was discarded by a codegen task (Project capture / incompatible opt-out):\n$out",
        )
        assertTrue(
            out.contains("Reusing configuration cache"),
            "expected the 2nd run to reuse the configuration cache, but it did not:\n$out",
        )
        // sanity: first run must have succeeded too
        assertTrue(first.output.isNotEmpty())
    }
}
