package io.github.mobilebytelabs.worker

expect object PlatformWorkManager {
    operator fun invoke(): WorkManager
}
