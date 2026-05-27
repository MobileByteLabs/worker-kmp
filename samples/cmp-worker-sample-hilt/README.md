# cmp-worker-sample-hilt — Hilt DI sample (v3.0.0-alpha07)

> Android Hilt DI sample demonstrating worker-kmp v3 via @HiltWorker pattern.
> Lands at v3.0.0-alpha07 per Phase 5 of the v3.0.0 epic.

## Current state (alpha04)

**SCAFFOLD ONLY.** Hilt sample app ships at v3.0.0-alpha07. This dir documents
the integration pattern that ALPHA07 will encode as a runnable APK.

## Hilt integration pattern (when shipped)

```kotlin
@HiltAndroidApp
class WorkerSampleHiltApp : Application(), Configuration.Provider {
    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
}

@HiltWorker
class HiltSyncWorker @AssistedInject constructor(
    @Assisted context: WorkerContext,
    private val repo: SyncRepository,
) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = TODO()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerBindingsModule {
    @Binds abstract fun bindWorkManager(impl: AndroidWorkManager): WorkManager
}
```

## Why a Hilt sample is needed

worker-kmp v3 standardizes on Koin via Phase 0's workKoinModule(). Hilt-only
consumers (Android-only apps that haven't adopted Koin) need a working sample
showing the Hilt-Koin bridge pattern. README guides the bridge; this sample is
the executable proof.

## Scope (when shipped)

Same two-tab UX as cmp-worker-sample-android (Scheduler + Monitor) but with
Hilt DI throughout instead of Koin. Same APK build target.
