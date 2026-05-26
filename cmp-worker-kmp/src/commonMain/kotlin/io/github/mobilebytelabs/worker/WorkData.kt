package io.github.mobilebytelabs.worker

/**
 * Immutable key-value store for passing data into and out of [CoroutineWorker] instances.
 *
 * Supported value types: [String], [Int], [Long], [Float], [Boolean], `Array<String>`.
 * Values of unsupported types are stored but will return `null` / the supplied default on read.
 *
 * Create instances with the [workDataOf] top-level function or the [WorkData.invoke] operator:
 * ```kotlin
 * val data = workDataOf("url" to "https://example.com", "retries" to 3)
 * val empty = WorkData.EMPTY
 * ```
 *
 * @see WorkRequest.inputData
 * @see WorkResult.Success.outputData
 * @see WorkInfo.outputData
 */
class WorkData private constructor(private val data: Map<String, Any?>) {

    /**
     * Returns the [String] value for [key], or [default] if the key is absent or the value
     * is not a [String].
     */
    fun getString(key: String, default: String? = null): String? = data[key] as? String ?: default

    /**
     * Returns the [Int] value for [key], or [default] if the key is absent or the value
     * cannot be coerced to an integer.
     */
    fun getInt(key: String, default: Int = 0): Int = (data[key] as? Number)?.toInt() ?: default

    /**
     * Returns the [Long] value for [key], or [default] if the key is absent or the value
     * cannot be coerced to a long.
     */
    fun getLong(key: String, default: Long = 0L): Long = (data[key] as? Number)?.toLong() ?: default

    /**
     * Returns the [Float] value for [key], or [default] if the key is absent or the value
     * cannot be coerced to a float.
     */
    fun getFloat(key: String, default: Float = 0f): Float = (data[key] as? Number)?.toFloat() ?: default

    /**
     * Returns the [Boolean] value for [key], or [default] if the key is absent or the value
     * is not a [Boolean].
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean = data[key] as? Boolean ?: default

    /**
     * Returns the `Array<String>` value for [key], or `null` if the key is absent or the
     * value is not an array of strings.
     */
    fun getStringArray(key: String): Array<String>? =
        (data[key] as? Array<*>)?.filterIsInstance<String>()?.toTypedArray()

    /** Returns `true` if this data contains a value mapped to [key]. */
    fun hasKey(key: String): Boolean = data.containsKey(key)

    /** Returns a defensive copy of the underlying map. */
    fun keyValueMap(): Map<String, Any?> = data.toMap()

    override fun equals(other: Any?): Boolean = other is WorkData && data == other.data
    override fun hashCode(): Int = data.hashCode()
    override fun toString(): String = "WorkData($data)"

    companion object {
        /** Singleton empty instance; use instead of allocating a new empty object. */
        val EMPTY: WorkData = WorkData(emptyMap())

        /** Creates a [WorkData] from the given key-value pairs. */
        operator fun invoke(vararg pairs: Pair<String, Any?>): WorkData = WorkData(pairs.toMap())
    }
}

/**
 * Creates a [WorkData] from the given key-value pairs.
 *
 * ```kotlin
 * val data = workDataOf("url" to "https://example.com", "page" to 1)
 * ```
 */
fun workDataOf(vararg pairs: Pair<String, Any?>): WorkData = WorkData(*pairs)
