package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.NetworkType
import kotlinx.coroutines.await
import kotlin.js.Promise

private fun jsOnline(): Boolean =
    js("typeof navigator !== 'undefined' && navigator.onLine === true") as Boolean

@Suppress("UnsafeCastFromDynamic")
private fun jsBatteryNotLowPromise(): Promise<Boolean> =
    js(
        """(function() {
            if (typeof navigator === 'undefined' || !('getBattery' in navigator)) return Promise.resolve(true);
            return navigator.getBattery().then(function(b) { return b.level > 0.2; });
        })()""",
    )

@Suppress("UnsafeCastFromDynamic")
private fun jsChargingPromise(): Promise<Boolean> =
    js(
        """(function() {
            if (typeof navigator === 'undefined' || !('getBattery' in navigator)) return Promise.resolve(true);
            return navigator.getBattery().then(function(b) { return b.charging === true; });
        })()""",
    )

@Suppress("UnsafeCastFromDynamic")
private fun jsStorageNotLowPromise(): Promise<Boolean> =
    js(
        """(function() {
            if (typeof navigator === 'undefined' || !navigator.storage || !navigator.storage.estimate)
                return Promise.resolve(true);
            return navigator.storage.estimate().then(function(e) {
                return (e.quota - e.usage) > 5242880;
            });
        })()""",
    )

internal actual fun defaultConstraintEvaluator(): WebConstraintEvaluator = WebConstraintEvaluator { constraints ->
    if (constraints.requiredNetworkType != NetworkType.NOT_REQUIRED && !jsOnline()) return@WebConstraintEvaluator false
    if (constraints.requiresBatteryNotLow && !jsBatteryNotLowPromise().await()) return@WebConstraintEvaluator false
    if (constraints.requiresCharging && !jsChargingPromise().await()) return@WebConstraintEvaluator false
    if (constraints.requiresStorageNotLow && !jsStorageNotLowPromise().await()) return@WebConstraintEvaluator false
    true
}
