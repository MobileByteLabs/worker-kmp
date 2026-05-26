package io.github.mobilebytelabs.worker

class WorkData private constructor(private val data: Map<String, Any?>) {

    fun getString(key: String, default: String? = null): String? = data[key] as? String ?: default
    fun getInt(key: String, default: Int = 0): Int = (data[key] as? Number)?.toInt() ?: default
    fun getLong(key: String, default: Long = 0L): Long = (data[key] as? Number)?.toLong() ?: default
    fun getFloat(key: String, default: Float = 0f): Float = (data[key] as? Number)?.toFloat() ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean = data[key] as? Boolean ?: default
    fun getStringArray(key: String): Array<String>? =
        (data[key] as? Array<*>)?.filterIsInstance<String>()?.toTypedArray()

    fun hasKey(key: String): Boolean = data.containsKey(key)
    fun keyValueMap(): Map<String, Any?> = data.toMap()

    override fun equals(other: Any?): Boolean = other is WorkData && data == other.data
    override fun hashCode(): Int = data.hashCode()
    override fun toString(): String = "WorkData($data)"

    companion object {
        val EMPTY: WorkData = WorkData(emptyMap())

        operator fun invoke(vararg pairs: Pair<String, Any?>): WorkData = WorkData(pairs.toMap())
    }
}

fun workDataOf(vararg pairs: Pair<String, Any?>): WorkData = WorkData(*pairs)
