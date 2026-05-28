# cmp-worker-sample-compose-store

End-to-end Compose Multiplatform sample that combines all four worker-kmp
integration libraries:

- **`cmp-worker-kmp`** — core `CoroutineWorker` / `WorkManager` API.
- **`cmp-worker-store5`** — `StoreBackedWorker<K, V>` driving a
  [Mobile Native Foundation Store5](https://github.com/MobileNativeFoundation/Store)
  store.
- **`cmp-worker-koin`** — single `workKoinModule(...)` call that wires the
  `WorkManager` backend + your `WorkerRegistry` into the DI graph.
- **`cmp-worker-compose`** — `WorkManagerProvider`, `LocalWorkManager`,
  `WorkInfoCard`, `BackgroundCapabilitiesBanner` — Compose-Multiplatform UI
  primitives that consume worker state directly.

## What it does

1. The user enters an article id and taps **Sync**.
2. `WorkManager.enqueue` schedules an `ArticleSyncWorker` (subclass of
   `StoreBackedWorker<String, Article>`).
3. The worker forces a fresh fetch on the shared Store5 store; the fake
   fetcher simulates network latency + an occasional transient error.
4. The UI observes **two** independent signals from the same underlying state:
   - the `WorkInfo` flow (tag = `article-sync`) drives a live `WorkInfoCard`,
   - the Store cached stream (`StoreReadRequest.cached`) drives an article
     details panel.

## Run

```bash
./gradlew :samples:cmp-worker-sample-compose-store:run
```

Targets JVM desktop (Compose Desktop). The `App()` composable lives entirely
in `commonMain` — to extend to iOS / wasmJs / Android, add the matching source
set + a platform-specific `WorkManagerFactory` (`iosWorkManagerFactory()`,
`webWorkManagerFactory()`, `androidWorkManagerFactory(context)`) into the
`startKoin { ... }` block.

## File map

| File | Layer |
|------|-------|
| `domain/Article.kt` | Domain model |
| `store/ArticlesStore.kt` | Store5 `Store<String, Article>` + fake fetcher |
| `workers/ArticleSyncWorker.kt` | `StoreBackedWorker` subclass |
| `di/AppModule.kt` | Koin modules (UI + worker registry) |
| `ui/App.kt` | Root composable + `WorkManagerProvider` |
| `ui/ArticlesScreen.kt` | Interactive screen |
| `jvmMain/Main.kt` | JVM entry — `startKoin` + `Window` + `App()` |
