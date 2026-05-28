package io.github.mobilebytelabs.worker.ios

/**
 * Configuration for [IosWorkManager].
 *
 * @param enableBackgroundTasks When `true`, the work manager registers a BGProcessingTask
 *   handler and schedules a background wake-up when constrained work is waiting. Requires
 *   [bgProcessingTaskIdentifier] to be set and the same identifier to be listed under
 *   `BGTaskSchedulerPermittedIdentifiers` in the host app's `Info.plist`.
 * @param bgProcessingTaskIdentifier The BGProcessingTask identifier to register and schedule.
 *   Must match an entry in `Info.plist → BGTaskSchedulerPermittedIdentifiers`.
 * @param enablePersistence When `true`, work state is written to NSUserDefaults so that
 *   pending work survives foreground/background transitions and app restarts.
 * @param persistenceKey NSUserDefaults key for stored work items. Override when multiple
 *   app extensions share the same NSUserDefaults suite to prevent collisions.
 */
data class IosWorkManagerConfig(
    val enableBackgroundTasks: Boolean = false,
    val bgProcessingTaskIdentifier: String = "",
    val enablePersistence: Boolean = true,
    val persistenceKey: String = "worker-kmp-ios",
    /**
     * BGAppRefreshTask identifier used by periodic workers that opt-in via
     * `PeriodicWorkRequestBuilder.setQuickRefresh(true)`. Must match an entry in
     * `Info.plist → BGTaskSchedulerPermittedIdentifiers` AND `UIBackgroundModes`
     * must include "fetch".
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    val appRefreshTaskIdentifier: String = "",
) {
    companion object {
        val DEFAULT = IosWorkManagerConfig()
    }
}
