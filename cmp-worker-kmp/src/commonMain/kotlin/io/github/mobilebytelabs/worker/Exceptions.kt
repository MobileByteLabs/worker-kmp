package io.github.mobilebytelabs.worker

/** Thrown when a [WorkRequest] cannot be accepted by the platform scheduler. */
class WorkEnqueueException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when the worker factory cannot create an instance of the requested [CoroutineWorker] class. */
class WorkerInstantiationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when [WorkData] cannot be serialized or deserialized by the platform. */
class WorkDataSerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)
