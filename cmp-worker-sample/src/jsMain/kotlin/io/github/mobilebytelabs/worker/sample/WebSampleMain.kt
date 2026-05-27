@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package io.github.mobilebytelabs.worker.sample

import io.github.mobilebytelabs.worker.BackoffPolicy
import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.NetworkType
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.RetryConfig
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.config.WebWorkerConfig
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.workerRegistry
import io.github.mobilebytelabs.worker.web.WebWorkManager
import io.github.mobilebytelabs.worker.web.isWebWorkManagerSupported
import io.github.mobilebytelabs.worker.web.webWorkManagerFactory
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

/**
 * Web (Kotlin/JS + Node.js) sample — demonstrates the worker-web API.
 *
 * Run: `./gradlew :cmp-worker-sample:jsNodeRun`
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor) — uses the
 * [webWorkManagerFactory] entry point instead of the removed `initWebWorkManager(...)`.
 * The factory is consumed by `workKoinModule(...)` for Koin-DI consumers; this sample
 * constructs the [WebWorkManager] directly to keep the scenario list focused on the
 * WorkManager API surface rather than DI plumbing.
 */
fun main() {
    // Progressive enhancement check — always true in browser/Node.js JS runtimes.
    if (!isWebWorkManagerSupported()) {
        println("Web platform not available in this runtime.")
        return
    }

    println("╔══════════════════════════════════════════╗")
    println("║    worker-kmp Web Sample (Node.js)        ║")
    println("╚══════════════════════════════════════════╝")
    println()

    // v3.0.0-alpha00.X deep-refactored API: construct the WorkManager directly via the
    // factory — no global PlatformWorkManager slot to read from anymore.
    val factory = webWorkManagerFactory()
    val config = WorkerConfig(
        webConfig = WebWorkerConfig(
            constraintCheckIntervalMs = 100,
            enablePersistence = false, // IndexedDB not available in Node.js
        ),
    )
    val workers = workerRegistry {
        register<WebImageResizeWorker> { ctx -> WebImageResizeWorker(ctx) }
        register<WebSyncWorker> { ctx -> WebSyncWorker(ctx) }
        register<WebHeartbeatWorker> { ctx -> WebHeartbeatWorker(ctx) }
    }
    val wm = factory.create(config, workers) as WebWorkManager

    MainScope().launch {
        webScenario1_oneTimeWork(wm)
        webScenario2_progressReporting(wm)
        webScenario3_retryWithBackoff(wm)
        webScenario4_uniquePeriodicWork(wm)
        webScenario5_keepPolicy(wm)
        webScenario6_networkConstraints(wm)
        println()
        println("All web scenarios complete.")
        wm.shutdown()
    }
}

// ── Scenario 1: One-time work with input/output ────────────────────────────────

private suspend fun webScenario1_oneTimeWork(wm: WebWorkManager) {
    println("── Scenario 1: One-time work with I/O ────────────────────")

    val req = OneTimeWorkRequestBuilder<WebImageResizeWorker>("WebImageResizeWorker")
        .setInputData(workDataOf("src" to "banner.jpg", "width" to 1920, "height" to 1080))
        .addTag("resize")
        .build()

    val id = wm.enqueue(req)
    println("  enqueued  id=${id.toString().take(8)}…")

    val info = wm.awaitFinished(id)
    if (info?.state == WorkInfo.State.SUCCEEDED) {
        println("  SUCCEEDED dst=${info.outputData.getString("dst")}")
    } else {
        println("  FAILED    state=${info?.state}")
    }
    println()
}

// ── Scenario 2: Progress reporting ────────────────────────────────────────────

private suspend fun webScenario2_progressReporting(wm: WebWorkManager) {
    println("── Scenario 2: Progress reporting ────────────────────────")

    val req = OneTimeWorkRequestBuilder<WebImageResizeWorker>("WebImageResizeWorker")
        .setInputData(workDataOf("src" to "photo.png", "width" to 800, "height" to 600))
        .addTag("render")
        .build()

    wm.enqueue(req)
    var lastProgress = -1
    repeat(100) {
        val info = wm.getWorkInfoById(req.id) ?: return@repeat
        val p = info.progress.progress
        if (p != lastProgress) {
            val msg = info.progress.data.getString("phase") ?: ""
            println("  progress  $p%  [$msg]")
            lastProgress = p
        }
        if (info.isFinished) return@repeat
        delay(20)
    }
    println("  state     ${wm.getWorkInfoById(req.id)?.state}")
    println()
}

// ── Scenario 3: Retry with exponential backoff ────────────────────────────────

private suspend fun webScenario3_retryWithBackoff(wm: WebWorkManager) {
    println("── Scenario 3: Retry with backoff ────────────────────────")
    WebSyncWorker.callCount = 0

    val req = OneTimeWorkRequestBuilder<WebSyncWorker>("WebSyncWorker")
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            RetryConfig(maxAttempts = 3, initialDelay = 50.milliseconds),
        )
        .addTag("sync")
        .build()

    val id = wm.enqueue(req)
    println("  enqueued  max-attempts=3 initial-delay=50ms")

    val info = wm.awaitFinished(id, timeoutMs = 5_000)
    println("  attempts  ${info?.runAttemptCount}")
    println("  state     ${info?.state}")
    println()
}

// ── Scenario 4: Unique periodic work ──────────────────────────────────────────

private suspend fun webScenario4_uniquePeriodicWork(wm: WebWorkManager) {
    println("── Scenario 4: Unique periodic work ──────────────────────")
    WebHeartbeatWorker.runCount = 0

    val req = PeriodicWorkRequestBuilder<WebHeartbeatWorker>(
        workerClass = "WebHeartbeatWorker",
        repeatInterval = 100.milliseconds,
    ).addTag("heartbeat").build()

    wm.enqueueUniquePeriodicWork("web-heartbeat", ExistingPeriodicWorkPolicy.REPLACE, req)
    println("  enqueued  interval=100ms")

    delay(850)
    println("  executions after ~850ms: ${WebHeartbeatWorker.runCount}")
    wm.cancelWorkById(req.id)
    println()
}

// ── Scenario 5: KEEP policy ───────────────────────────────────────────────────

private suspend fun webScenario5_keepPolicy(wm: WebWorkManager) {
    println("── Scenario 5: KEEP policy ────────────────────────────────")

    val req1 = PeriodicWorkRequestBuilder<WebHeartbeatWorker>(
        "WebHeartbeatWorker",
        repeatInterval = 5.minutes,
    ).addTag("keep-demo").build()
    val req2 = PeriodicWorkRequestBuilder<WebHeartbeatWorker>(
        "WebHeartbeatWorker",
        repeatInterval = 5.minutes,
    ).addTag("keep-demo").build()

    val id1 = wm.enqueueUniquePeriodicWork("keep-demo", ExistingPeriodicWorkPolicy.KEEP, req1)
    val id2 = wm.enqueueUniquePeriodicWork("keep-demo", ExistingPeriodicWorkPolicy.KEEP, req2)
    println("  first  id=${id1.toString().take(8)}…")
    println("  second id=${id2.toString().take(8)}…  (same: ${id1 == id2})")
    wm.cancelAllWorkByTag("keep-demo")
    println()
}

// ── Scenario 6: Network constraint declaration ────────────────────────────────

private suspend fun webScenario6_networkConstraints(wm: WebWorkManager) {
    println("── Scenario 6: Network constraint ─────────────────────────")

    val req = OneTimeWorkRequestBuilder<WebImageResizeWorker>("WebImageResizeWorker")
        .setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
        .setInputData(workDataOf("src" to "thumb.jpg", "width" to 120, "height" to 90))
        .addTag("constrained")
        .build()

    val id = wm.enqueue(req)
    val info = wm.awaitFinished(id, timeoutMs = 3_000)
    // In Node.js the default evaluator treats CONNECTED as satisfied (no browser API check).
    println("  CONNECTED constraint → ${info?.state}")
    println()
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private suspend fun WebWorkManager.awaitFinished(id: Uuid, timeoutMs: Long = 4_000): WorkInfo? {
    val intervalMs = 30L
    val maxAttempts = (timeoutMs / intervalMs).toInt()
    repeat(maxAttempts) {
        val info = getWorkInfoById(id) ?: return null
        if (info.isFinished) return info
        delay(intervalMs)
    }
    return getWorkInfoById(id)
}

// ── Workers ───────────────────────────────────────────────────────────────────

class WebImageResizeWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val src = inputData.getString("src") ?: return WorkResult.failure("missing src")
        val width = inputData.getInt("width")
        val height = inputData.getInt("height")
        setProgress(WorkProgress(0, workDataOf("phase" to "Decoding")))
        delay(30)
        setProgress(WorkProgress(50, workDataOf("phase" to "Resizing")))
        delay(30)
        setProgress(WorkProgress(100, workDataOf("phase" to "Done")))
        val dst = src.replace(".", "_${width}x$height.")
        return WorkResult.success(workDataOf("dst" to dst, "width" to width, "height" to height))
    }
}

class WebSyncWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        delay(30)
        return if (++callCount < 3) {
            WorkResult.retry("simulated timeout (attempt $callCount)")
        } else {
            WorkResult.success(workDataOf("records" to 42L))
        }
    }

    companion object {
        var callCount = 0
    }
}

class WebHeartbeatWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        runCount++
        return WorkResult.success(workDataOf("tick" to runCount))
    }

    companion object {
        var runCount = 0
    }
}
