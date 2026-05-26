@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.NetworkType

@JsFun("() => typeof navigator !== 'undefined' && navigator.onLine === true")
private external fun jsNavigatorOnline(): Boolean

// Battery Status API is async (Promise-based) — not directly bindable via @JsFun.
// Return true as a conservative pass-through; callers wanting strict battery gating
// should supply a custom WebConstraintEvaluator via the internal constructor.
@JsFun("() => true")
private external fun jsBatteryNotLow(): Boolean

@JsFun("() => true")
private external fun jsCharging(): Boolean

// StorageManager.estimate() is also async; conservative pass-through for wasmJs.
@JsFun("() => true")
private external fun jsStorageNotLow(): Boolean

internal actual fun defaultConstraintEvaluator(): WebConstraintEvaluator = WebConstraintEvaluator { constraints ->
    if (constraints.requiredNetworkType != NetworkType.NOT_REQUIRED && !jsNavigatorOnline()) return@WebConstraintEvaluator false
    if (constraints.requiresBatteryNotLow && !jsBatteryNotLow()) return@WebConstraintEvaluator false
    if (constraints.requiresCharging && !jsCharging()) return@WebConstraintEvaluator false
    if (constraints.requiresStorageNotLow && !jsStorageNotLow()) return@WebConstraintEvaluator false
    true
}
