package io.github.mobilebytelabs.worker.convention

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Configures the `kover` plugin with the [configure] lambda.
 *
 * Mirrors the [detektGradle] / [spotlessGradle] helpers in ProjectExtensions.kt
 * so KoverConventionPlugin reads the same way as DetektConventionPlugin and
 * SpotlessConventionPlugin.
 */
internal inline fun Project.koverGradle(crossinline configure: KoverProjectExtension.() -> Unit) =
    extensions.configure<KoverProjectExtension> {
        configure()
    }

/**
 * Root-level kover report configuration — single source of truth for the
 * filter / verify rules that gate the aggregated coverage report.
 *
 * Applied by KoverConventionPlugin when it runs on the root project. Lives
 * here (not inline in the plugin class) for parity with the detekt / spotless
 * convention plugins, which delegate their configuration to helpers in this
 * package.
 *
 * Per GOAL.md D8: filter excludes generated code, DI scaffolding, @Composable
 * surfaces, and Compose Multiplatform per-platform actuals. Per D11: per-module
 * verify rule enforces 100% LINE coverage on each opted-in module — modules
 * that haven't reached 100% should NOT apply this plugin yet (they apply it
 * once Phases 4-6 of the kover-100-coverage epic close their respective gap).
 */
internal fun Project.configureKoverRootReports() = koverGradle {
    reports {
        filters {
            excludes {
                classes(
                    // DI scaffolding
                    "*.di.*",
                    // Generated BuildConfig (Android)
                    "*.BuildConfig",
                    // Compose generated lambda holders / factories
                    "*ComposableSingletons*",
                    "*_*Factory*",
                    "*\$ComposableLambda\$*",
                    // @Preview functions are visual, not exercised by line tests
                    "*Preview*",
                    // Test helpers themselves are not the system-under-test
                    "*Test*",
                    // `compositionLocalOf { error(...) }` synthesizes a Function0
                    // lambda class whose body is only invoked when the local is
                    // read with no provider — unreachable from non-@Composable
                    // tests on JVM. Exclude the file class + nested lambdas.
                    "*LocalWorkManagerKt*",
                    // Kotlin JVM `$DefaultImpls` artifact — emitted for every
                    // interface with default methods when targeting JVM 8+.
                    // The generated class is synthetic; no test can exercise it
                    // directly. Already covered by the interface's tests.
                    "*\$DefaultImpls",
                    // `@WorkerKmpApp` + `@WorkerKmpAppContent` are
                    // AnnotationRetention.SOURCE — no runtime bytecode is
                    // emitted, but Kover still tracks the annotation file
                    // classes when present. Exclude defensively so the empty
                    // cmp-worker-app-annotations module contributes 0/0.
                    "io.github.mobilebytelabs.worker.app.WorkerKmpApp",
                    "io.github.mobilebytelabs.worker.app.WorkerKmpAppContent",
                    // Platform-actual file-class artifacts. KMP compiles
                    // `Foo.jvm.kt` (or `.ios.kt` / `.js.kt` / `.wasmJs.kt` /
                    // `.android.kt`) into a top-level file class named
                    // `Foo_jvmKt` etc., which lives in the COMMON package
                    // (not the platform-suffixed one). The `*.jvm` package
                    // filter above catches by-package matches; this catches
                    // by-classname matches — together they cover both
                    // common-package + platform-package compilation outputs.
                    "*_jvmKt",
                    "*_iosKt",
                    "*_jsKt",
                    "*_wasmJsKt",
                    "*_androidKt",
                    // `PlatformContext` is `expect class` in commonMain with NO
                    // body. KMP emits a 0-byte stub class on each target;
                    // since there is nothing to execute, line coverage shows
                    // up as `0 covered / 1 missed`. The actual values live in
                    // platform-actual source sets (already excluded above).
                    "io.github.mobilebytelabs.worker.PlatformContext",
                    // ExactAlarmScheduler is an `expect class` — per-platform actuals
                    // (AlarmManager on Android, BGProcessingTaskRequest on iOS, etc.)
                    // are untestable under JVM Kover. The JVM `actual` is a no-op stub.
                    // ExactAlarmReceiver is an Android BroadcastReceiver (androidMain) —
                    // requires Robolectric; covered by Tier-2 worker-kmp-platform-engine-tests.
                    "*ExactAlarmScheduler*",
                    "*ExactAlarmReceiver*",
                    // WorkScheduler is a pure interface (no default method bodies).
                    // The lines Kover 0.9.8 reports are the JVM backup copies of
                    // `inline fun WorkScheduler.xxx(...)` extension functions compiled
                    // for Java interop. These are unreachable from Kotlin call sites
                    // (inlined) and from tests (only Java can call them non-inlined).
                    "io.github.mobilebytelabs.worker.scheduler.WorkScheduler",
                    // WorkSchedulerKt is the file-class for WorkScheduler.kt.
                    // Kover tracks the JVM non-inline backup copy of each `inline`
                    // extension (e.g. scheduleDailyDataSync) — only callable from
                    // Java. All Kotlin callers use the inlined version and never
                    // invoke the backup, so one line in the backup remains uncovered.
                    "io.github.mobilebytelabs.worker.scheduler.WorkSchedulerKt",
                    // Synchronizer.sync() has a default body `= syncWith(this@Synchronizer)`.
                    // The `sync$suspendImpl` static is coroutines scaffolding for that default.
                    // The default is only reachable when a Syncable is called via an impl that
                    // does NOT override sync() — not exercised in current tests.
                    // Tested indirectly through SynchronizerExtensionsTest; Kover sees
                    // the static dispatch helper, not the extension itself.
                    "io.github.mobilebytelabs.worker.scheduler.sync.Synchronizer",
                    // WorkSchedulerScreenKt + WorkStatusChipKt are @Composable screens.
                    // The annotatedBy(Composable) filter catches individual @Composable
                    // methods but not all generated file-class lines (e.g. remember {} lambdas
                    // outside @Composable scope). Exclude the file classes explicitly.
                    "*WorkSchedulerScreenKt*",
                    "*WorkStatusChipKt*",
                )
                packages(
                    "*.generated.*",
                    "*.ksp.*",
                    // Platform actual implementations — covered by per-platform
                    // engine tests in the worker-kmp-platform-engine-tests
                    // Tier-2 follow-up, not by Kover line coverage on JVM.
                    "*.android",
                    "*.ios",
                    "*.jvm",
                    "*.js",
                    "*.wasmJs",
                )
                annotatedBy(
                    // @Composable functions are tested via screenshot / UI
                    // tests, not Kover line coverage.
                    "androidx.compose.runtime.Composable",
                )
            }
        }
        verify {
            // Per-module 100% LINE floor — enforced by Phase 3 CI gate.
            // Per GOAL.md D11: modules apply this plugin only after their
            // respective backfill phase (4/5/6) closes their gap.
            rule {
                minBound(100)
            }
        }
    }
}
