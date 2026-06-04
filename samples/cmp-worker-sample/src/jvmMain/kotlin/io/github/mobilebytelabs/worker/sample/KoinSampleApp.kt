@file:OptIn(io.github.mobilebytelabs.worker.koin.WorkerKmpInternalApi::class)

package io.github.mobilebytelabs.worker.sample

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.desktop.desktopWorkManagerFactory
import io.github.mobilebytelabs.worker.koin.workKoinModulePrivateApi
import io.github.mobilebytelabs.worker.oneTimeWorkRequest
import io.github.mobilebytelabs.worker.registry.workerRegistry
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin

// ---------------------------------------------------------------------------
// A simple repository injected into a worker via Koin
// ---------------------------------------------------------------------------
interface GreetingRepository {
    fun greet(name: String): String
}

class DefaultGreetingRepository : GreetingRepository {
    override fun greet(name: String) = "Hello, $name! (from Koin-injected repo)"
}

// ---------------------------------------------------------------------------
// Worker that receives its dependency from Koin (not hardcoded)
// ---------------------------------------------------------------------------
class KoinGreetingWorker(context: WorkerContext, private val repo: GreetingRepository) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val name = inputData.getString("name") ?: return WorkResult.failure()
        val message = repo.greet(name)
        println("  Worker executed: $message")
        return WorkResult.success(workDataOf("message" to message))
    }
}

// ---------------------------------------------------------------------------
// App Koin module — only app-specific bindings now; worker registration lives
// inside workKoinModule(workers = ...)
// ---------------------------------------------------------------------------
val appModule = module {
    single<GreetingRepository> { DefaultGreetingRepository() }
}

fun main() = runBlocking {
    println("╔══════════════════════════════════════════╗")
    println("║       worker-kmp × Koin Sample           ║")
    println("╚══════════════════════════════════════════╝")
    println()

    // v3.0.0-alpha00.X deep-refactored API: a single startKoin call wires both the
    // WorkManager backend (via desktopWorkManagerFactory) and the consumer registry.
    // No more pre-startKoin initializeWorkerDesktop(...) step — that legacy entry
    // point + the PlatformWorkManager global slot have been removed outright.
    startKoin {
        modules(
            workKoinModulePrivateApi(
                config = WorkerConfig(),
                workers = workerRegistry {
                    register<KoinGreetingWorker> { ctx ->
                        KoinGreetingWorker(ctx, getKoin().get())
                    }
                },
                factory = desktopWorkManagerFactory(),
            ),
            appModule,
        )
    }

    println("Koin started — WorkManager and GreetingRepository registered.")
    println()

    // 3. Resolve WorkManager from Koin and enqueue work
    val workManager: WorkManager = getKoin().get()

    val id = workManager.enqueue(
        oneTimeWorkRequest<KoinGreetingWorker> {
            setInputData(workDataOf("name" to "Kotlin Multiplatform"))
            addTag("koin-demo")
        },
    )

    println("Enqueued KoinGreetingWorker (id=$id) — waiting for result…")

    // 4. Observe until terminal
    val result = workManager.getWorkInfosByTag("koin-demo")
        .first { infos -> infos.any { it.state.isFinished } }
        .first { it.state.isFinished }

    println("Work finished: state=${result.state}, output=${result.outputData}")
    println()
    println("✓ Koin DI demo complete.")

    stopKoin()
}
