package io.github.mobilebytelabs.worker.android

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import io.github.mobilebytelabs.worker.PlatformWorkManager

/**
 * Initializes the KMP WorkManager for Android.
 *
 * Call once in your `Application.onCreate()` **before** any work is enqueued.
 *
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         initializeWorkerAndroid(this)
 *     }
 * }
 * ```
 *
 * If you need a custom [KmpAndroidWorkerFactory] (e.g. for DI), pass it as [workerFactory].
 * By default, workers are instantiated via reflection.
 *
 * Note: this disables the automatic WorkManager initializer. Add
 * `<provider android:name="androidx.startup.InitializationProvider" tools:node="remove" />`
 * or use `androidx.work.impl.WorkManagerInitializer` removal if you call this manually.
 */
fun initializeWorkerAndroid(context: Context, workerFactory: KmpAndroidWorkerFactory = ReflectionKmpWorkerFactory) {
    val factory = KmpWorkerFactory(workerFactory)
    WorkManager.initialize(
        context,
        Configuration.Builder()
            .setWorkerFactory(factory)
            .build(),
    )
    PlatformWorkManager.configure(AndroidWorkManager(context))
}
