package io.github.mobilebytelabs.worker.bc

import io.github.mobilebytelabs.worker.WorkManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * Phase 13 infrastructure smoke test. Confirms the dual-classpath setup actually
 * resolves `io.github.mobilebytelabs.worker.WorkManager` from both classpath flavors:
 *   - v2BcTest: from worker-kmp:2.1.0 (Maven Central)
 *   - v3BcTest: from current local build
 *
 * If this test fails on one classpath but passes on the other, the dual-classpath
 * gradle config is broken. Real per-deprecation tests are added by Phase 0 when
 * the v2.x init wrappers become @Deprecated.
 */
class V2BaselineSmokeTest {

    @Test
    fun workManagerInterface_resolves_onBothClasspaths() {
        // The interface type itself — present in both v2.1.0 and v3.x
        val ifaceClass = WorkManager::class.java
        assertNotNull(ifaceClass, "WorkManager interface must resolve from active classpath")
    }
}
