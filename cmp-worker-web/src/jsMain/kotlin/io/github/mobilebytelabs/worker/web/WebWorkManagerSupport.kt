package io.github.mobilebytelabs.worker.web

@Suppress("UnsafeCastFromDynamic")
actual fun isWebWorkManagerSupported(): Boolean =
    js("typeof window !== 'undefined' || typeof self !== 'undefined'") as Boolean
