package io.github.mobilebytelabs.worker.sample

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.desktop.DesktopWorkerFactory
import io.github.mobilebytelabs.worker.desktop.initializeWorkerDesktop
import io.github.mobilebytelabs.worker.koin.workKoinModule
import io.github.mobilebytelabs.worker.oneTimeWorkRequest
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
// App Koin module — registers both the repository and WorkManager
// ---------------------------------------------------------------------------
val appModule = module {
    single<GreetingRepository> { DefaultGreetingRepository() }
}

fun main() = runBlocking {
    println("╔══════════════════════════════════════════╗")
    println("║       worker-kmp × Koin Sample           ║")
    println("╚══════════════════════════════════════════╝")
    println()

    // 1. Init desktop WorkManager with a factory that delegates to Koin
    initializeWorkerDesktop(
        workerFactory = object : DesktopWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker {
                val koin = getKoin()
                return when (workerClass) {
                    "KoinGreetingWorker" -> KoinGreetingWorker(context, koin.get())
                    else -> error("Unknown worker: $workerClass")
                }
            }
        },
    )

    // 2. Start Koin with workKoinModule (provides WorkManager) + appModule (provides repo)
    startKoin { modules(workKoinModule, appModule) }

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
