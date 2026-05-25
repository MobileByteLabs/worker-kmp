package io.github.mobilebytelabs.worker

import kotlin.test.Test
import kotlin.test.assertContains

class JvmGreetingTest {

    @Test
    fun testJvmPlatformName() {
        val platformName = getPlatformName()
        assertContains(platformName, "JVM")
    }
}
