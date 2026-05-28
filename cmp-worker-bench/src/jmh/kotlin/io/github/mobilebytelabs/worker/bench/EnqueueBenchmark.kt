/*
 * Phase 11 (PLAN worker-kmp-v3-foreground-storeflow-11-performance-benchmarks).
 *
 * Micro-benchmarks for `WorkManager.enqueue(OneTimeWorkRequest)` on `TestWorkManager`.
 * Measures the cost of constructing + scheduling a request through the public API surface.
 *
 * Targeting:
 *   - v2.1.0-current surfaces only (TestWorkManager + oneTimeWorkRequest builder + Constraints DSL).
 *   - JVM-only — platform-specific schedulers measured in later phases as they land.
 */
package io.github.mobilebytelabs.worker.bench

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.NetworkType
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.oneTimeWorkRequest
import io.github.mobilebytelabs.worker.test.TestWorkManager
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit
import kotlin.uuid.Uuid

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
open class EnqueueBenchmark {

    private lateinit var workManager: TestWorkManager

    @Setup(Level.Trial)
    fun setup() {
        workManager = TestWorkManager()
    }

    @Benchmark
    fun enqueueOneTime(): Uuid = runBlocking {
        workManager.enqueue(oneTimeWorkRequest<NoOpWorker> { })
    }

    @Benchmark
    fun enqueueOneTimeWithConstraints(): Uuid = runBlocking {
        workManager.enqueue(
            oneTimeWorkRequest<NoOpWorker> {
                setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
            },
        )
    }
}

class NoOpWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}
