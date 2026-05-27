/*
 * Phase 11 (PLAN worker-kmp-v3-foreground-storeflow-11-performance-benchmarks).
 *
 * Micro-benchmarks for `TestWorkManager` in-memory state operations at N=10/100/1000.
 *
 * This is the ONLY persistence surface that exists in v2.1.0 — file-based persistence
 * (Phase 8 desktop daemon) and IndexedDB persistence (Phase 9 web) benchmarks land
 * with their respective phases. The intent here is to anchor the in-memory baseline
 * so later persistence layers can be compared against the no-IO floor.
 *
 * Measures the cost of looking up a `WorkInfo` from the in-memory store after the
 * store has been populated to a given size. Setup populates the store with N tagged
 * requests; the benchmark fetches the last enqueued ID via `getWorkInfoById`.
 */
package io.github.mobilebytelabs.worker.bench

import io.github.mobilebytelabs.worker.WorkInfo
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
import org.openjdk.jmh.annotations.Param
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
open class PersistenceBenchmark {

    @Param("10", "100", "1000")
    var n: Int = 0

    private lateinit var workManager: TestWorkManager
    private lateinit var lastId: Uuid

    @Setup(Level.Trial)
    fun setup() {
        workManager = TestWorkManager()
        runBlocking {
            var last: Uuid? = null
            repeat(n) {
                last = workManager.enqueue(oneTimeWorkRequest<NoOpWorker> { addTag("bench-persist") })
            }
            lastId = requireNotNull(last) { "n must be > 0 for setup" }
        }
    }

    @Benchmark
    fun getWorkInfoById(): WorkInfo? = runBlocking {
        workManager.getWorkInfoById(lastId)
    }
}
