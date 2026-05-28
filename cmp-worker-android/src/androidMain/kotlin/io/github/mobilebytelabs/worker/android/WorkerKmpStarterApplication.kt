package io.github.mobilebytelabs.worker.android

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Base [Application] that bootstraps Koin idempotently with the modules returned by [koinModules].
 *
 * Consumer usage:
 *
 * ```
 * class SampleApplication : WorkerKmpStarterApplication() {
 *     override fun koinModules() = sampleKoinModules(androidWorkManagerFactory(this))
 * }
 * ```
 *
 * Register the subclass via `AndroidManifest.xml`:
 *
 * ```xml
 * <application android:name=".SampleApplication">
 *     ...
 * </application>
 * ```
 *
 * On [onCreate], `startKoin` is invoked exactly once — subsequent calls (e.g. process restart
 * while Koin was already initialized by an instrumented test fixture) are no-ops, so the
 * pattern is safe to combine with test infrastructure that pre-initializes Koin.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md` AC4.
 */
public abstract class WorkerKmpStarterApplication : Application() {

    /**
     * Return the Koin modules to register on application startup.
     * Typically a list combining your `appModule` plus any worker-kmp Koin modules
     * (e.g. `workModule(androidWorkManagerFactory(this))`).
     *
     * Named `koinModules` (not `modules`) to avoid shadowing Koin's own DSL
     * `modules(...)` function inside the `startKoin { ... }` block.
     */
    protected abstract fun koinModules(): List<Module>

    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@WorkerKmpStarterApplication)
                modules(koinModules())
            }
        }
    }
}
