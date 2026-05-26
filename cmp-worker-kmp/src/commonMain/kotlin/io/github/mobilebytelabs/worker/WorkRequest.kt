package io.github.mobilebytelabs.worker

import kotlin.time.Duration
import kotlin.uuid.Uuid

sealed class WorkRequest {
    abstract val id: Uuid
    abstract val workerClass: String
    abstract val inputData: WorkData
    abstract val constraints: Constraints
    abstract val retryConfig: RetryConfig
    abstract val tags: Set<String>
}

@ConsistentCopyVisibility
data class OneTimeWorkRequest internal constructor(
    override val id: Uuid,
    override val workerClass: String,
    override val inputData: WorkData,
    override val constraints: Constraints,
    override val retryConfig: RetryConfig,
    override val tags: Set<String>,
) : WorkRequest()

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
) : WorkRequest()

class OneTimeWorkRequestBuilder<T : CoroutineWorker>(private val workerClass: String) {
    private var inputData: WorkData = WorkData.EMPTY
    private var constraints: Constraints = Constraints.NONE
    private var retryConfig: RetryConfig = RetryConfig.DEFAULT
    private val tags: MutableSet<String> = mutableSetOf()
    private var id: Uuid = Uuid.random()

    fun setInputData(data: WorkData): OneTimeWorkRequestBuilder<T> = apply { inputData = data }
    fun setConstraints(c: Constraints): OneTimeWorkRequestBuilder<T> = apply { constraints = c }
    fun setBackoffCriteria(policy: BackoffPolicy, config: RetryConfig): OneTimeWorkRequestBuilder<T> =
        apply { retryConfig = config }
    fun addTag(tag: String): OneTimeWorkRequestBuilder<T> = apply { tags.add(tag) }
    fun setId(id: Uuid): OneTimeWorkRequestBuilder<T> = apply { this.id = id }

    fun build(): OneTimeWorkRequest = OneTimeWorkRequest(
        id = id,
        workerClass = workerClass,
        inputData = inputData,
        constraints = constraints,
        retryConfig = retryConfig,
        tags = tags.toSet(),
    )
}

class PeriodicWorkRequestBuilder<T : CoroutineWorker>(
    private val workerClass: String,
    private val repeatInterval: Duration,
    private val flexTimeInterval: Duration = Duration.ZERO,
) {
    private var inputData: WorkData = WorkData.EMPTY
    private var constraints: Constraints = Constraints.NONE
    private val tags: MutableSet<String> = mutableSetOf()

    fun setInputData(data: WorkData): PeriodicWorkRequestBuilder<T> = apply { inputData = data }
    fun setConstraints(c: Constraints): PeriodicWorkRequestBuilder<T> = apply { constraints = c }
    fun addTag(tag: String): PeriodicWorkRequestBuilder<T> = apply { tags.add(tag) }

    fun build(): PeriodicWorkRequest = PeriodicWorkRequest(
        id = Uuid.random(),
        workerClass = workerClass,
        inputData = inputData,
        constraints = constraints,
        retryConfig = RetryConfig.DEFAULT,
        tags = tags.toSet(),
        repeatInterval = repeatInterval,
        flexTimeInterval = flexTimeInterval,
    )
}

inline fun <reified T : CoroutineWorker> oneTimeWorkRequest(
    block: OneTimeWorkRequestBuilder<T>.() -> Unit = {},
): OneTimeWorkRequest = OneTimeWorkRequestBuilder<T>(T::class.simpleName ?: "Unknown").apply(block).build()

inline fun <reified T : CoroutineWorker> periodicWorkRequest(
    repeatInterval: Duration,
    flexTimeInterval: Duration = Duration.ZERO,
    block: PeriodicWorkRequestBuilder<T>.() -> Unit = {},
): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<T>(T::class.simpleName ?: "Unknown", repeatInterval, flexTimeInterval)
        .apply(block)
        .build()
