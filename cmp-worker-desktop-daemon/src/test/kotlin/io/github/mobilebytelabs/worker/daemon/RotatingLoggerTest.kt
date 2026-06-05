package io.github.mobilebytelabs.worker.daemon

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.logging.Level

class RotatingLoggerTest {

    @Test
    fun install_doesNotThrow(@TempDir tmp: Path) {
        RotatingLogger.install(tmp.toFile().absolutePath)
    }

    @Test
    fun install_calledTwice_isIdempotent(@TempDir tmp: Path) {
        val dir = tmp.toFile().absolutePath
        RotatingLogger.install(dir)
        RotatingLogger.install(dir)
    }

    @Test
    fun install_nonWritablePathSimulated_doesNotThrow() {
        RotatingLogger.install("/dev/null/impossible-path")
    }

    @Test
    fun log_afterInstall_doesNotThrow(@TempDir tmp: Path) {
        RotatingLogger.install(tmp.toFile().absolutePath)
        RotatingLogger.log(Level.INFO, "test message from RotatingLoggerTest")
    }

    @Test
    fun log_beforeInstall_doesNotThrow() {
        RotatingLogger.log(Level.WARNING, "pre-install log call should be no-op")
    }
}
