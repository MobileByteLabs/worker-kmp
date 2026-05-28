@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.mobilebytelabs.worker.ios

import co.touchlab.kermit.Logger
import platform.Foundation.NSBundle

/**
 * Inspects the host app's `Info.plist` against the active [IosWorkManagerConfig] and
 * logs actionable kermit ERROR entries when required entries are missing.
 *
 * Does NOT throw — log-only — so a misconfigured Info.plist degrades gracefully
 * (background tasks silently no-op rather than crashing the app at launch).
 *
 * Checks performed:
 * - When `config.enableBackgroundTasks` is true:
 *   - `UIBackgroundModes` must contain `"processing"`.
 *   - `BGTaskSchedulerPermittedIdentifiers` must contain `bgProcessingTaskIdentifier`.
 * - When `config.appRefreshTaskIdentifier` is non-empty:
 *   - `UIBackgroundModes` must contain `"fetch"`.
 *   - `BGTaskSchedulerPermittedIdentifiers` must contain `appRefreshTaskIdentifier`.
 *
 * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
 */
internal fun validateInfoPlist(config: IosWorkManagerConfig) {
    val info = NSBundle.mainBundle.infoDictionary ?: run {
        Logger.withTag(LOG_TAG).w {
            "Info.plist not accessible via NSBundle.mainBundle — skipping background-tasks validation."
        }
        return
    }
    val backgroundModes = info[KEY_UI_BACKGROUND_MODES] as? List<*>
    val permittedIds = info[KEY_BG_TASK_PERMITTED_IDS] as? List<*>

    if (config.enableBackgroundTasks) {
        if (config.bgProcessingTaskIdentifier.isEmpty()) {
            Logger.withTag(LOG_TAG).e {
                "enableBackgroundTasks=true but bgProcessingTaskIdentifier is empty. " +
                    "Set it to match an entry in Info.plist → BGTaskSchedulerPermittedIdentifiers."
            }
        } else {
            requireBackgroundMode(backgroundModes, MODE_PROCESSING, capability = "BGProcessingTask")
            requirePermittedIdentifier(permittedIds, config.bgProcessingTaskIdentifier)
        }
    }

    if (config.appRefreshTaskIdentifier.isNotEmpty()) {
        requireBackgroundMode(backgroundModes, MODE_FETCH, capability = "BGAppRefreshTask (quickRefresh)")
        requirePermittedIdentifier(permittedIds, config.appRefreshTaskIdentifier)
    }
}

private const val LOG_TAG = "worker-kmp.ios.infoplist"
private const val KEY_UI_BACKGROUND_MODES = "UIBackgroundModes"
private const val KEY_BG_TASK_PERMITTED_IDS = "BGTaskSchedulerPermittedIdentifiers"
private const val MODE_PROCESSING = "processing"
private const val MODE_FETCH = "fetch"

private fun requireBackgroundMode(modes: List<*>?, expected: String, capability: String) {
    val ok = modes?.any { (it as? String) == expected } == true
    if (!ok) {
        Logger.withTag(LOG_TAG).e {
            "$capability requires Info.plist → UIBackgroundModes to contain \"$expected\". " +
                "Add <string>$expected</string> under <key>UIBackgroundModes</key>."
        }
    }
}

private fun requirePermittedIdentifier(permittedIds: List<*>?, identifier: String) {
    val ok = permittedIds?.any { (it as? String) == identifier } == true
    if (!ok) {
        Logger.withTag(LOG_TAG).e {
            "Info.plist → BGTaskSchedulerPermittedIdentifiers must contain \"$identifier\". " +
                "Add it under <key>BGTaskSchedulerPermittedIdentifiers</key> as an <array> entry."
        }
    }
}
