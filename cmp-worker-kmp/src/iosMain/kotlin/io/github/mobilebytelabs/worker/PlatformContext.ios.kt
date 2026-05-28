package io.github.mobilebytelabs.worker

/**
 * On iOS, no platform context is required — workers run in the app's main process.
 */
public actual class PlatformContext public constructor()
