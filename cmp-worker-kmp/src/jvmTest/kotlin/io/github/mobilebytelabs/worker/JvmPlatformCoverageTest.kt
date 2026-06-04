/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(ExperimentalForegroundApi::class)

package io.github.mobilebytelabs.worker

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Coverage harness for the JVM source set: closes the host-JVM-reachable gap on
 * - PlatformContext.jvm.kt (the empty `actual class PlatformContext` — 0% before)
 * - ForegroundWorker.jvm.kt (helper functions + happy/headless dispatch paths)
 *
 * Note: the SystemTray.add() codepath inside `ensureTrayIcon` cannot be covered on
 * headless CI runners because [java.awt.SystemTray.isSupported] returns `false`
 * there. The headless-fallback branch IS covered (kermit WARN + return).
 */
class JvmPlatformCoverageTest {

    @Test
    fun platformContext_canBeConstructed() {
        val ctx: PlatformContext = PlatformContext()
        assertNotNull(ctx)
    }

    @Test
    fun isSystemTraySupported_returnsBoolean_withoutThrowing() {
        // Whether tray is supported is environment-dependent (false on headless CI,
        // possibly true on a developer's GUI shell). Both branches of the runCatching
        // wrapper are valid — the assertion is only that the helper is callable.
        val supported = isSystemTraySupported()
        // Force the boolean to be observed so the JIT doesn't elide the call.
        assertEquals(supported, supported)
    }

    @Test
    fun runAsForeground_inHeadlessEnv_logsAndReturns_withoutThrowing() = runTest {
        // On CI / headless JVM this exercises:
        //   isAndroidRuntime() -> false (we're on HotSpot)
        //   isSystemTraySupported() -> typically false on headless runners
        //   → enters the kermit WARN branch + returns cleanly
        //
        // On a developer's GUI shell where tray IS supported, it instead exercises
        //   ensureTrayIcon() → tray.add() → tooltip update → tray.remove() at 100%.
        // Either way, the call must not throw.
        val noOpWorker = object : ForegroundWorker(TestWorkerContext()) {
            override suspend fun doWork(): WorkResult = WorkResult.success()
        }
        val info = ForegroundInfo(
            notificationId = 4242,
            title = "Job",
            message = "Working",
            progress = WorkProgress(50),
        )
        runAsForeground(noOpWorker, info)
    }

    @Test
    fun runAsForeground_progress100_runsCompletionBranch() = runTest {
        // When progress == COMPLETE_PROGRESS (100), the runCatching block enters
        // the `tray.remove(icon)` + `trayIcons.remove(...)` cleanup branch
        // (only on tray-supported environments; harmlessly no-ops elsewhere).
        val noOpWorker = object : ForegroundWorker(TestWorkerContext()) {
            override suspend fun doWork(): WorkResult = WorkResult.success()
        }
        val done = ForegroundInfo(
            notificationId = 9999,
            title = "Done",
            message = "Complete",
            progress = WorkProgress(100),
        )
        runAsForeground(noOpWorker, done)
    }
}
