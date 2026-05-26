package io.github.mobilebytelabs.worker.android

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    addListener(
        {
            try {
                if (isCancelled) {
                    cont.cancel()
                } else {
                    cont.resume(get())
                }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        },
        { command -> command.run() },
    )
    cont.invokeOnCancellation { cancel(false) }
}
