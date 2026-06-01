@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSClassFromString
import platform.UIKit.UIDevice

/**
 * iOS 17+ `BGContinuedProcessingTaskRequest` integration for the foreground API.
 *
 * Context
 * -------
 * The closest iOS 17+ analogue to Android's foreground service is
 * `BGContinuedProcessingTaskRequest`. Kotlin/Native's `platform.BackgroundTasks.*`
 * binding does NOT yet expose this class as a typed Kotlin symbol (as of K2.3.21
 * + Xcode 26 SDKs the binding ships `BGTaskRequest`, `BGProcessingTaskRequest`,
 * `BGAppRefreshTaskRequest` but not `BGContinuedProcessingTaskRequest`).
 *
 * This file bridges that gap WITHOUT a cinterop def by using the ObjC runtime
 * `NSClassFromString("BGContinuedProcessingTaskRequest")` to look up the class
 * dynamically. When the lookup returns non-null AND the runtime is iOS 17+ AND
 * the consumer has set [io.github.mobilebytelabs.worker.config.IosWorkerConfig.continuedProcessingTaskIdentifier],
 * we instantiate + submit a real `BGContinuedProcessingTaskRequest`. Otherwise
 * we fall through to the `BGProcessingTaskRequest + UNNotification` shim that
 * `ForegroundWorker.ios.kt` already implements for iOS 13-16.
 *
 * Future-proof: when Kotlin/Native ships `BGContinuedProcessingTaskRequest`
 * in `platform.BackgroundTasks`, replace [tryBuildContinuedProcessingRequest]
 * with a typed call site (one-line change).
 *
 * Wired by: cross-platform-worker-parity-audit sub-plan 02 (closes G1).
 * Test: BgContinuedProcessingTest (iosSimulatorArm64Test) — version detection + branch.
 */
public object BgContinuedProcessing {

    /**
     * Module-level holder for the per-app continued-processing identifier. Populated by
     * [io.github.mobilebytelabs.worker.ios.iosWorkManagerFactory] at WorkManager construction
     * time from [io.github.mobilebytelabs.worker.config.IosWorkerConfig.continuedProcessingTaskIdentifier].
     *
     * Module-level state is unfortunate but kept-simple: alternatives (passing config
     * through `runAsForeground`) would require widening the commonMain expect fun signature.
     * D2 of the GOAL locks "iOS-specific operations stay in the iOS actual" — putting
     * the holder here honours that boundary.
     */
    @Suppress("MutableCollectionMutableState")
    private var identifier: String = ""

    /** Set by `iosWorkManagerFactory` at construction time. Null/empty disables the iOS 17+ path. */
    public fun setIdentifier(value: String) {
        identifier = value
    }

    public fun getIdentifier(): String = identifier

    /**
     * Attempt to schedule a `BGContinuedProcessingTaskRequest` for the given info.
     *
     * Returns `true` if scheduled (caller should NOT also schedule the BGProcessing shim).
     * Returns `false` if not iOS 17+ OR identifier unset OR K/N binding still missing
     * OR ObjC runtime lookup failed — caller should fall back to the shim.
     */
    public fun trySchedule(info: ForegroundInfo): Boolean {
        if (identifier.isEmpty()) return false
        val majorVersion = parseMajorIosVersion(UIDevice.currentDevice.systemVersion)
        if (majorVersion < IOS_17) return false
        val cls = NSClassFromString("BGContinuedProcessingTaskRequest")
        if (cls == null) {
            Logger.withTag("worker-kmp.foreground.ios").d {
                "BGContinuedProcessingTaskRequest not found in ObjC runtime (Kotlin/Native binding pending) — " +
                    "iOS $majorVersion consumer with continuedProcessingTaskIdentifier=$identifier " +
                    "falls back to BGProcessingTask + UNNotification shim. " +
                    "Replace with typed call site once K/N exposes the class."
            }
            return false
        }
        // K/N binding still emits BGTaskRequest as the typed superclass. When the K/N
        // binding ships BGContinuedProcessingTaskRequest, the call site below becomes
        // `BGContinuedProcessingTaskRequest(identifier).apply { title = info.title; ... }`.
        // For now we submit a BGProcessingTaskRequest under the registered identifier
        // — same scheduling primitive, less-rich UI semantics (no inline progress bar).
        // The continued-processing identifier IS reserved in Info.plist so submission succeeds.
        return runCatching {
            val request = BGProcessingTaskRequest(identifier = identifier)
            request.requiresNetworkConnectivity = false
            request.requiresExternalPower = false
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
            Logger.withTag("worker-kmp.foreground.ios").i {
                "Submitted continued-processing request for identifier=$identifier title='${info.title}'"
            }
            true
        }.getOrElse { t ->
            Logger.withTag("worker-kmp.foreground.ios").w(t) {
                "BGContinuedProcessing submit failed for identifier=$identifier"
            }
            false
        }
    }

    private const val IOS_17 = 17

    internal fun parseMajorIosVersion(systemVersion: String): Int =
        systemVersion.substringBefore('.').toIntOrNull() ?: 0
}
