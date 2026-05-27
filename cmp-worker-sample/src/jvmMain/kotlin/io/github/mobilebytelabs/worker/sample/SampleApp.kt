package io.github.mobilebytelabs.worker.sample

import io.github.mobilebytelabs.worker.BackoffPolicy
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.RetryConfig
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.desktop.DesktopWorkManager
import io.github.mobilebytelabs.worker.desktop.DesktopWorkManagerConfig
import io.github.mobilebytelabs.worker.desktop.DesktopWorkerFactory
import io.github.mobilebytelabs.worker.sample.workers.CacheCleanupWorker
import io.github.mobilebytelabs.worker.sample.workers.DataSyncWorker
import io.github.mobilebytelabs.worker.sample.workers.ImageResizeWorker
import io.github.mobilebytelabs.worker.sample.workers.LongRunningWorker
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

fun main() = runBlocking {
    println("╔══════════════════════════════════════════╗")
    println("║         worker-kmp Sample App            ║")
    println("╚══════════════════════════════════════════╝")
    println()

    val wm = DesktopWorkManager(
        config = DesktopWorkManagerConfig.IN_MEMORY,
        workerFactory = SampleWorkerFactory,
    )

    scenario1_oneTimeTaskWithIO(wm)
    scenario2_progressReporting(wm)
    scenario3_retryWithBackoff(wm)
    scenario4_uniquePeriodicWork(wm)
    scenario5_cancellation(wm)

    wm.shutdown()
    println()
    println("All scenarios complete.")
}

// ── Scenario 1: One-time task with input / output data ────────────────────────

private suspend fun scenario1_oneTimeTaskWithIO(wm: DesktopWorkManager) {
    println("── Scenario 1: One-time task with I/O data ──────────────")

    val req = OneTimeWorkRequestBuilder<ImageResizeWorker>("ImageResizeWorker")
        .setInputData(workDataOf("src" to "photo.jpg", "width" to 800, "height" to 600))
        .addTag("resize")
        .build()

    val id = wm.enqueue(req)
    println("  enqueued  id=${id.toString().take(8)}…")

    val info = wm.awaitFinished(id)
    if (info?.state == WorkInfo.State.SUCCEEDED) {
        val out = info.outputData
        println("  SUCCEEDED dst=${out.getString("dst")}  ${out.getInt("width")}x${out.getInt("height")}")
    } else {
        println("  FAILED    state=${info?.state}")
    }
    println()
}

// ── Scenario 2: Progress reporting ───────────────────────────────────────────

private suspend fun scenario2_progressReporting(wm: DesktopWorkManager) {
    println("── Scenario 2: Progress reporting ───────────────────────")

    val req = OneTimeWorkRequestBuilder<ImageResizeWorker>("ImageResizeWorker")
        .setInputData(workDataOf("src" to "banner.png", "width" to 1920, "height" to 1080))
        .addTag("render")
        .build()

    wm.enqueue(req)

    var lastProgress = -1
    val deadline = System.currentTimeMillis() + 5_000
    while (System.currentTimeMillis() < deadline) {
        val info = wm.getWorkInfoById(req.id) ?: break
        val p = info.progress.progress
        if (p != lastProgress) {
            println("  progress  $p%")
            lastProgress = p
        }
        if (info.isFinished) break
        delay(20)
    }

    val final = wm.getWorkInfoById(req.id)
    println("  state     ${final?.state}")
    println()
}

// ── Scenario 3: Automatic retry with exponential backoff ─────────────────────

private suspend fun scenario3_retryWithBackoff(wm: DesktopWorkManager) {
    println("── Scenario 3: Retry with backoff ───────────────────────")
    DataSyncWorker.callCount.set(0)

    val retryConfig = RetryConfig(
        maxAttempts = 3,
        initialDelay = 100.milliseconds,
        backoffPolicy = BackoffPolicy.EXPONENTIAL,
    )
    val req = OneTimeWorkRequestBuilder<DataSyncWorker>("DataSyncWorker")
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, retryConfig)
        .addTag("sync")
        .build()

    val id = wm.enqueue(req)
    println("  enqueued  max-attempts=${retryConfig.maxAttempts} initial-delay=${retryConfig.initialDelay}")

    val info = wm.awaitFinished(id, timeoutMs = 10_000)
    println("  attempts  ${info?.runAttemptCount}")
    println("  state     ${info?.state}")
    if (info?.state == WorkInfo.State.SUCCEEDED) {
        println("  output    records=${info.outputData.getLong("recordCount")}")
    }
    println()
}

// ── Scenario 4: Unique periodic work ─────────────────────────────────────────

private suspend fun scenario4_uniquePeriodicWork(wm: DesktopWorkManager) {
    println("── Scenario 4: Unique periodic work ─────────────────────")
    CacheCleanupWorker.runCount.set(0)

    val req = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
        workerClass = "CacheCleanupWorker",
        repeatInterval = 200.milliseconds,
    ).addTag("cleanup").build()

    wm.enqueueUniquePeriodicWork("cache-cleanup", ExistingPeriodicWorkPolicy.REPLACE, req)
    println("  enqueued  interval=200ms")

    val deadline = System.currentTimeMillis() + 1_500
    while (System.currentTimeMillis() < deadline) {
        val count = CacheCleanupWorker.runCount.get()
        if (count >= 3) break
        delay(50)
    }

    val runs = CacheCleanupWorker.runCount.get()
    println("  executions after ~1.5s: $runs")

    wm.cancelWorkById(req.id)

    val info = wm.getWorkInfosByTag("cleanup").first()
    println("  state     ${info.firstOrNull { it.id == req.id }?.state}")
    println()
}

// ── Scenario 5: Cancellation ──────────────────────────────────────────────────

private suspend fun scenario5_cancellation(wm: DesktopWorkManager) {
    println("── Scenario 5: Cancellation ──────────────────────────────")

    val req = OneTimeWorkRequestBuilder<LongRunningWorker>("LongRunningWorker")
        .addTag("long")
        .build()

    val id = wm.enqueue(req)
    println("  enqueued  (60-second task)")

    delay(100)
    println("  cancelling…")
    wm.cancelWorkById(id)

    val info = wm.awaitFinished(id)
    println("  state     ${info?.state}")
    println()
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private suspend fun DesktopWorkManager.awaitFinished(id: Uuid, timeoutMs: Long = 8_000): WorkInfo? {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val info = getWorkInfoById(id) ?: return null
        if (info.isFinished) return info
        delay(30)
    }
    return getWorkInfoById(id)
}

// ── Worker factory ────────────────────────────────────────────────────────────

public object SampleWorkerFactory : DesktopWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker = when (workerClass) {
        "ImageResizeWorker" -> ImageResizeWorker(context)
        "DataSyncWorker" -> DataSyncWorker(context)
        "CacheCleanupWorker" -> CacheCleanupWorker(context)
        "LongRunningWorker" -> LongRunningWorker(context)
        else -> error("Unknown worker class: $workerClass")
    }
}
