package io.github.mobilebytelabs.worker.daemon

import co.touchlab.kermit.Logger
import java.util.ServiceLoader

/**
 * Daemon-side worker factory contract.
 *
 * Consumer apps that want their workers to run via the OS-scheduler daemon (when the
 * consumer process is not running) register a [DaemonWorkerFactory] implementation
 * via Java [ServiceLoader] — i.e. add a line to
 * `META-INF/services/io.github.mobilebytelabs.worker.daemon.DaemonWorkerFactory`
 * pointing at the FQCN of an implementation in the consumer's daemon classpath.
 *
 * When the daemon JAR boots and scans the persistence dir for ENQUEUED work, it
 * loads all available [DaemonWorkerFactory] instances via ServiceLoader. For each
 * persisted work item with a known `workerClass`, the daemon iterates factories
 * until one returns non-null for [create].
 *
 * No factory found → daemon falls back to the pre-G4 healing-only behaviour
 * (flip stuck RUNNING → ENQUEUED so the consumer app picks it up on next launch).
 *
 * Added by cross-platform-worker-parity-audit sub-plan 05 (closes G4).
 */
public interface DaemonWorkerFactory {
    /**
     * Instantiate + execute the worker identified by [workerClass] with [inputDataJson].
     *
     * Returns `true` on successful dispatch (work ran; state should be flipped to a
     * terminal state by the daemon based on the returned [DaemonWorkResult]).
     * Returns `null` if this factory doesn't recognize [workerClass] — daemon will
     * try the next registered factory.
     */
    public suspend fun create(workerClass: String, inputDataJson: String): DaemonWorkResult?
}

/** Outcome of a daemon-side worker run. */
public sealed class DaemonWorkResult {
    public object Success : DaemonWorkResult()
    public data class Failure(val reason: String) : DaemonWorkResult()
    public data class Retry(val reason: String) : DaemonWorkResult()
}

/**
 * Loads + dispatches via [ServiceLoader]-registered [DaemonWorkerFactory] instances.
 *
 * Internal to the daemon module — `DesktopBackgroundDaemon` invokes this when scanning
 * for ENQUEUED work to dispatch directly (vs. the pre-G4 healing-only behaviour).
 */
internal object DaemonWorkerDispatch {
    private val log = Logger.withTag("worker-kmp.daemon.dispatch")

    private val factories: List<DaemonWorkerFactory> by lazy {
        val loaded = ServiceLoader.load(DaemonWorkerFactory::class.java).toList()
        log.i { "Loaded ${loaded.size} DaemonWorkerFactory implementation(s) via ServiceLoader" }
        loaded
    }

    /**
     * Try every registered factory in order. Returns the first non-null result, or
     * null if no factory recognized [workerClass] (daemon should heal-only).
     */
    suspend fun dispatch(workerClass: String, inputDataJson: String): DaemonWorkResult? {
        for (factory in factories) {
            val result = runCatching { factory.create(workerClass, inputDataJson) }
                .getOrElse { t ->
                    log.w(t) {
                        "Factory ${factory::class.qualifiedName} threw on workerClass=$workerClass — trying next"
                    }
                    null
                }
            if (result != null) {
                log.d {
                    "Dispatched workerClass=$workerClass via ${factory::class.qualifiedName} → ${result::class.simpleName}"
                }
                return result
            }
        }
        log.d {
            "No factory recognized workerClass=$workerClass (${factories.size} factories tried) — daemon will heal-only"
        }
        return null
    }
}
