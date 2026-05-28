package io.github.mobilebytelabs.worker

import kotlin.time.Duration
import kotlin.uuid.Uuid

/**
 * Base class for all work requests submitted to [WorkManager].
 *
 * Use the typed builder DSL functions instead of constructing subclasses directly:
 * - [oneTimeWorkRequest] — runs the worker exactly once.
 * - [periodicWorkRequest] — runs the worker on a repeating schedule.
 *
 * @property id unique identifier for this request; auto-generated unless overridden via
 *   [OneTimeWorkRequestBuilder.setId].
 * @property workerClass fully-qualified or simple class name of the [CoroutineWorker] to run.
 * @property inputData key-value payload passed to [CoroutineWorker.inputData].
 * @property constraints platform conditions that must be met before execution begins.
 * @property retryConfig retry behaviour when [CoroutineWorker.doWork] returns [WorkResult.Retry].
 * @property tags arbitrary labels for grouping, observation, and bulk cancellation.
 */
sealed class WorkRequest {
    abstract val id: Uuid
    abstract val workerClass: String
    abstract val inputData: WorkData
    abstract val constraints: Constraints
    abstract val retryConfig: RetryConfig
    abstract val tags: Set<String>
}

/**
 * A work request whose worker runs exactly once.
 *
 * Construct via [oneTimeWorkRequest] or [OneTimeWorkRequestBuilder].
 *
 * @property id unique identifier for this request.
 * @property workerClass class name of the [CoroutineWorker] to execute.
 * @property inputData key-value data available inside the worker.
 * @property constraints execution prerequisites (network, charging, etc.).
 * @property retryConfig retry behaviour on [WorkResult.Retry].
 * @property tags labels for observation and bulk cancellation.
 * @property initialDelay how long to wait before first attempt; defaults to [Duration.ZERO].
 *   Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
 * @property expeditedPolicy when non-null, requests Android 12+ expedited execution.
 *   [OutOfQuotaPolicy] controls behaviour when the expedited quota is exhausted.
 *   No-op on iOS/Desktop/Web. Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
 */
@ConsistentCopyVisibility
data class OneTimeWorkRequest internal constructor(
    override val id: Uuid,
    override val workerClass: String,
    override val inputData: WorkData,
    override val constraints: Constraints,
    override val retryConfig: RetryConfig,
    override val tags: Set<String>,
    val initialDelay: Duration = Duration.ZERO,
    val expeditedPolicy: OutOfQuotaPolicy? = null,
) : WorkRequest()

/**
 * A work request whose worker runs repeatedly on a fixed interval.
 *
 * Construct via [periodicWorkRequest] or [PeriodicWorkRequestBuilder].
 *
 * The flex window `[repeatInterval - flexTimeInterval, repeatInterval]` gives the platform
 * scheduler freedom to batch work for battery efficiency. Pass [Duration.ZERO] for exact-interval
 * scheduling (not recommended on battery-constrained devices).
 *
 * @property id unique identifier for this request.
 * @property workerClass class name of the [CoroutineWorker] to execute.
 * @property inputData key-value data available inside the worker.
 * @property constraints execution prerequisites (network, charging, etc.).
 * @property retryConfig retry behaviour on [WorkResult.Retry].
 * @property tags labels for observation and bulk cancellation.
 * @property repeatInterval how often the worker should run.
 * @property flexTimeInterval scheduling flex window; defaults to [Duration.ZERO].
 */
@ConsistentCopyVisibility
data class PeriodicWorkRequest internal constructor(
    override val id: Uuid,
    override val workerClass: String,
    override val inputData: WorkData,
    override val constraints: Constraints,
    override val retryConfig: RetryConfig,
    override val tags: Set<String>,
    val repeatInterval: Duration,
    val flexTimeInterval: Duration = Duration.ZERO,
    val initialDelay: Duration = Duration.ZERO,
    val quickRefresh: Boolean = false,
) : WorkRequest()

/**
 * Builder for [OneTimeWorkRequest].
 *
 * Prefer the [oneTimeWorkRequest] DSL function for concise construction.
 *
 * @param T the [CoroutineWorker] subclass that will execute this request.
 * @param workerClass simple or fully-qualified class name; populated automatically by [oneTimeWorkRequest].
 */
class OneTimeWorkRequestBuilder<T : CoroutineWorker>(private val workerClass: String) {
    private var inputData: WorkData = WorkData.EMPTY
    private var constraints: Constraints = Constraints.NONE
    private var retryConfig: RetryConfig = RetryConfig.DEFAULT
    private val tags: MutableSet<String> = mutableSetOf()
    private var id: Uuid = Uuid.random()
    private var initialDelay: Duration = Duration.ZERO
    private var expeditedPolicy: OutOfQuotaPolicy? = null

    /** Sets the key-value payload passed to the worker as [CoroutineWorker.inputData]. */
    fun setInputData(data: WorkData): OneTimeWorkRequestBuilder<T> = apply { inputData = data }

    /** Sets the platform conditions that must be satisfied before execution begins. */
    fun setConstraints(c: Constraints): OneTimeWorkRequestBuilder<T> = apply { constraints = c }

    /** Sets the retry strategy; [policy] is accepted for API symmetry with AndroidX WorkManager. */
    fun setBackoffCriteria(policy: BackoffPolicy, config: RetryConfig): OneTimeWorkRequestBuilder<T> =
        apply { retryConfig = config }

    /** Adds a tag for grouping, observation, or bulk cancellation. */
    fun addTag(tag: String): OneTimeWorkRequestBuilder<T> = apply { tags.add(tag) }

    /** Overrides the auto-generated request ID (useful for idempotency). */
    fun setId(id: Uuid): OneTimeWorkRequestBuilder<T> = apply { this.id = id }

    /**
     * Sets the initial delay before the first attempt.
     *
     * Per-platform behaviour:
     * - **Android**: maps to `androidx.work.WorkRequest.Builder.setInitialDelay(...)`.
     * - **iOS**: sets `BGProcessingTaskRequest.earliestBeginDate` when background tasks
     *   are enabled; otherwise the platform polling loop honours the delay via `delay()`.
     * - **Desktop**: scheduler waits via coroutine `delay()` before invoking `doWork()`.
     * - **Web**: scheduler waits via coroutine `delay()` before invoking `doWork()`.
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    fun setInitialDelay(delay: Duration): OneTimeWorkRequestBuilder<T> = apply { initialDelay = delay }

    /**
     * Requests Android 12+ expedited execution. [policy] controls behaviour when the
     * expedited-work quota is exhausted (see [OutOfQuotaPolicy]).
     *
     * Per-platform behaviour:
     * - **Android (API 31+)**: maps to `androidx.work.OneTimeWorkRequest.Builder.setExpedited(...)`.
     * - **Android (<31)**: no-op (logged at debug level).
     * - **iOS / Desktop / Web**: no-op (these platforms have no expedited-work concept).
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    fun setExpedited(policy: OutOfQuotaPolicy): OneTimeWorkRequestBuilder<T> = apply { expeditedPolicy = policy }

    /** Constructs the immutable [OneTimeWorkRequest]. */
    fun build(): OneTimeWorkRequest = OneTimeWorkRequest(
        id = id,
        workerClass = workerClass,
        inputData = inputData,
        constraints = constraints,
        retryConfig = retryConfig,
        tags = tags.toSet(),
        initialDelay = initialDelay,
        expeditedPolicy = expeditedPolicy,
    )
}

/**
 * Builder for [PeriodicWorkRequest].
 *
 * Prefer the [periodicWorkRequest] DSL function for concise construction.
 *
 * @param T the [CoroutineWorker] subclass that will execute this request.
 * @param workerClass simple or fully-qualified class name; populated automatically by [periodicWorkRequest].
 * @param repeatInterval how often the worker should run.
 * @param flexTimeInterval scheduling flex window; defaults to [Duration.ZERO].
 */
class PeriodicWorkRequestBuilder<T : CoroutineWorker>(
    private val workerClass: String,
    private val repeatInterval: Duration,
    private val flexTimeInterval: Duration = Duration.ZERO,
) {
    private var inputData: WorkData = WorkData.EMPTY
    private var constraints: Constraints = Constraints.NONE
    private val tags: MutableSet<String> = mutableSetOf()
    private var initialDelay: Duration = Duration.ZERO
    private var quickRefresh: Boolean = false

    /** Sets the key-value payload passed to the worker as [CoroutineWorker.inputData]. */
    fun setInputData(data: WorkData): PeriodicWorkRequestBuilder<T> = apply { inputData = data }

    /** Sets the platform conditions that must be satisfied before execution begins. */
    fun setConstraints(c: Constraints): PeriodicWorkRequestBuilder<T> = apply { constraints = c }

    /** Adds a tag for grouping, observation, or bulk cancellation. */
    fun addTag(tag: String): PeriodicWorkRequestBuilder<T> = apply { tags.add(tag) }

    /**
     * Sets the initial delay before the first attempt. See [OneTimeWorkRequestBuilder.setInitialDelay].
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    fun setInitialDelay(delay: Duration): PeriodicWorkRequestBuilder<T> = apply { initialDelay = delay }

    /**
     * iOS quick-refresh hint. When `true`, the iOS scheduler uses
     * `BGAppRefreshTaskRequest` (short, frequent wake-ups) instead of
     * `BGProcessingTaskRequest` (longer, less frequent). Requires
     * `appRefreshTaskIdentifier` to be set on `IosWorkerConfig` AND the matching
     * identifier in `Info.plist → BGTaskSchedulerPermittedIdentifiers`.
     *
     * No-op on Android / Desktop / Web.
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    fun setQuickRefresh(enabled: Boolean): PeriodicWorkRequestBuilder<T> = apply { quickRefresh = enabled }

    /** Constructs the immutable [PeriodicWorkRequest]. */
    fun build(): PeriodicWorkRequest = PeriodicWorkRequest(
        id = Uuid.random(),
        workerClass = workerClass,
        inputData = inputData,
        constraints = constraints,
        retryConfig = RetryConfig.DEFAULT,
        tags = tags.toSet(),
        repeatInterval = repeatInterval,
        flexTimeInterval = flexTimeInterval,
        initialDelay = initialDelay,
        quickRefresh = quickRefresh,
    )
}

/**
 * Creates a [OneTimeWorkRequest] for worker type [T] using a builder DSL.
 *
 * ```kotlin
 * val request = oneTimeWorkRequest<SyncWorker> {
 *     setInputData(workDataOf("url" to endpoint))
 *     setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
 *     addTag("sync")
 * }
 * ```
 *
 * @param T the [CoroutineWorker] subclass to run.
 * @param block optional builder configuration block.
 */
inline fun <reified T : CoroutineWorker> oneTimeWorkRequest(
    block: OneTimeWorkRequestBuilder<T>.() -> Unit = {},
): OneTimeWorkRequest = OneTimeWorkRequestBuilder<T>(T::class.simpleName ?: "Unknown").apply(block).build()

/**
 * Creates a [PeriodicWorkRequest] for worker type [T] using a builder DSL.
 *
 * ```kotlin
 * val request = periodicWorkRequest<HeartbeatWorker>(repeatInterval = 15.minutes) {
 *     setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
 *     addTag("heartbeat")
 * }
 * ```
 *
 * @param T the [CoroutineWorker] subclass to run.
 * @param repeatInterval how often the worker should run.
 * @param flexTimeInterval scheduling flex window; defaults to [Duration.ZERO].
 * @param block optional builder configuration block.
 */
inline fun <reified T : CoroutineWorker> periodicWorkRequest(
    repeatInterval: Duration,
    flexTimeInterval: Duration = Duration.ZERO,
    block: PeriodicWorkRequestBuilder<T>.() -> Unit = {},
): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<T>(T::class.simpleName ?: "Unknown", repeatInterval, flexTimeInterval)
        .apply(block)
        .build()
