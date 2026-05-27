/*
 * Phase 11 (PLAN worker-kmp-v3-foreground-storeflow-11-performance-benchmarks).
 *
 * STUB — placeholder benchmark file for the WorkObserver SPI overhead measurement.
 *
 * The `WorkObserver` SPI lands with Phase 4 (PLAN worker-kmp-v3-foreground-storeflow-04-work-observer-spi)
 * in v2.2.0+alpha07. Until then, this file is intentionally empty so the
 * `cmp-worker-bench` module compiles cleanly and CI wiring stays exercised, but
 * no benchmarks are registered.
 *
 * When Phase 4 ships, the populated benchmark class will measure:
 *   - Observer registration cost (per-observer)
 *   - Dispatch fanout cost at N=1, N=10, N=100 observers
 *   - Allocation overhead per WorkInfo state change
 *
 * Re-enable by uncommenting the @State class below, importing the WorkObserver SPI,
 * and adding setup/teardown for the observer fixtures.
 */
package io.github.mobilebytelabs.worker.bench

// @State(Scope.Benchmark)
// @BenchmarkMode(Mode.AverageTime)
// @OutputTimeUnit(TimeUnit.MICROSECONDS)
// @Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
// @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
// @Fork(value = 3)
// open class ObserverChainBenchmark {
//     // lands when Phase 4 ships WorkObserver SPI in v2.2.0+alpha07.
// }
