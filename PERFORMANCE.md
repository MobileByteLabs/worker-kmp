# Performance

> Measurable baselines for every hot path in worker-kmp. CI fails on >20% regression
> vs the latest release baseline. See [`perf-baselines/`](perf-baselines/) for raw JMH
> JSON per release.

## Metrics tracked (initial — v2.2.0)

| Surface | Metric | Target (acceptable) | Regression threshold |
|---|---|---|---|
| `TestWorkManager.enqueue(OneTimeWorkRequest)` (no constraints) | wall-clock μs (avg) | <500μs | >20% |
| `TestWorkManager.enqueue(OneTimeWorkRequest)` (CONNECTED constraint) | wall-clock μs (avg) | <500μs | >20% |
| `TestWorkManager` in-memory state ops at N=100 | wall-clock μs (avg) | <100μs | >20% |

Additional metrics ship at later milestones as the relevant surfaces land:

- Observer chain overhead → v2.2.0 + alpha07 (Phase 4 WorkObserver SPI)
- Daemon cold-start + roundtrip → v3.0.0-alpha05 (Phase 8 Desktop daemon)
- Web Push wake → SW dispatch → v3.0.0-alpha06 (Phase 9 Web Push)
- Per-platform enqueue (Android / iOS / Web Wasm) → as each lands

## How we benchmark

JMH (Java Microbenchmark Harness) 1.37 with:
- Mode: `AverageTime` + `Throughput`
- Forks: 3
- Warmup: 5 iterations × 2s
- Measurement: 5 iterations × 3s
- Output unit: microseconds (μs)

Runs on GitHub-hosted `ubuntu-latest` runners pinned to JDK temurin 21 + Gradle 9.5.1.

## Reading the numbers

JMH reports score (mean) ± scoreError (confidence interval, 99.9%). Headline: `score` in `scoreUnit`. Lower is better for AverageTime; higher is better for Throughput.

## Capturing a new baseline

```bash
bash scripts/perf-capture-baseline.sh <version>
git add perf-baselines/<version>.json
git commit -m "perf(bench): capture v<version> baseline"
```

## Running benchmarks locally

```bash
./gradlew :cmp-worker-bench:jmh                           # full run (~15 min)
./gradlew :cmp-worker-bench:jmh -Pjmh.iterations=2 \
    -Pjmh.warmupIterations=2 -Pjmh.fork=1                  # quick run (~3 min)
```
