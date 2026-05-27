# Performance Baselines

JMH-captured baselines per release. CI compares PR runs against the latest released baseline; fails on >20% regression for any metric.

## Format

JMH JSON output (`-Pjmh.resultFormat=JSON`). One file per release: `{version}.json`.

## CI runner spec

Baselines are captured on `ubuntu-latest` GitHub-hosted runners (specifically: `actions/runner-images/Ubuntu24` family). Cross-runner-class comparison is noisy — pin baselines + PR runs to the same class.

JDK: temurin 21. Gradle: 9.5.1. JMH: 1.37.

## Capturing a new baseline

```bash
bash scripts/perf-capture-baseline.sh <version>
git add perf-baselines/<version>.json
git commit -m "perf(bench): capture v<version> baseline"
```

## Reading the JSON

JMH JSON has `score` (mean) + `scoreError` (CI) + `scoreUnit` per benchmark. Look at `primaryMetric.score` for the headline number.
