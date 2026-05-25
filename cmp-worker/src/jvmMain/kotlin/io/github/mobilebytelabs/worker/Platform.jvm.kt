package io.github.mobilebytelabs.worker

actual fun getPlatformName(): String = "JVM ${System.getProperty("java.version")}"
