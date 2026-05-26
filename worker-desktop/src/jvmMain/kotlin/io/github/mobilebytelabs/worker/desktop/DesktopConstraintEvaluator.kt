package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress

internal class DesktopConstraintEvaluator(private val config: DesktopWorkManagerConfig) {

    suspend fun evaluate(constraints: Constraints): Boolean {
        if (constraints == Constraints.NONE) return true

        return coroutineScope {
            val networkOk = async(Dispatchers.IO) {
                when (constraints.requiredNetworkType) {
                    NetworkType.NOT_REQUIRED -> true
                    else -> checkNetworkConnectivity()
                }
            }
            val storageOk = async(Dispatchers.IO) {
                if (constraints.requiresStorageNotLow) checkStorageNotLow() else true
            }
            networkOk.await() && storageOk.await()
        }
    }

    private fun checkNetworkConnectivity(): Boolean = try {
        InetAddress.getByName("8.8.8.8").isReachable(3_000)
    } catch (_: IOException) {
        false
    }

    private fun checkStorageNotLow(): Boolean =
        config.persistencePath.getUsableSpace() > 10L * 1024 * 1024
}
