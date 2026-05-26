package io.github.mobilebytelabs.worker.android

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkerContext

/**
 * Android [WorkerFactory] that routes all KMP work through [KmpAndroidWorker].
 *
 * Install via `WorkManager.initialize(context, Configuration.Builder().setWorkerFactory(factory).build())`.
 */
class KmpWorkerFactory(
    private val kmpFactory: KmpAndroidWorkerFactory = ReflectionKmpWorkerFactory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        if (workerClassName != KmpAndroidWorker::class.qualifiedName) return null
        instance = kmpFactory
        return KmpAndroidWorker(appContext, workerParameters)
    }

    companion object {
        internal var instance: KmpAndroidWorkerFactory? = null
    }
}

interface KmpAndroidWorkerFactory {
    fun create(workerClass: String, context: WorkerContext): CoroutineWorker
}

internal object ReflectionKmpWorkerFactory : KmpAndroidWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker {
        val clazz = Class.forName(workerClass)
        val ctor = clazz.getDeclaredConstructor(WorkerContext::class.java)
        return ctor.newInstance(context) as CoroutineWorker
    }
}
