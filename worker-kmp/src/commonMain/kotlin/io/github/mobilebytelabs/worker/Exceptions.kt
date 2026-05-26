package io.github.mobilebytelabs.worker

class WorkEnqueueException(message: String, cause: Throwable? = null) : Exception(message, cause)
class WorkerInstantiationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class WorkDataSerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)
