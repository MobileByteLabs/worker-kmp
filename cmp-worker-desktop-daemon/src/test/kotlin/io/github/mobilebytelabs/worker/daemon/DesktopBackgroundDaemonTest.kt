package io.github.mobilebytelabs.worker.daemon

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.Properties

class DesktopBackgroundDaemonTest {

    @Test
    fun main_probeFlag_doesNotThrow() {
        main(arrayOf("--probe"))
    }

    @Test
    fun main_emptyPersistenceDir_doesNotThrow(@TempDir tmp: Path) {
        main(arrayOf("--persistence-dir", tmp.toFile().absolutePath, "--max-runtime-seconds", "1"))
    }

    @Test
    fun main_persistenceDirWithEnqueuedWork_logsAndExits(@TempDir tmp: Path) {
        val dir = tmp.toFile()
        val props = Properties()
        props.setProperty("state", "ENQUEUED")
        val file = File(dir, "work-001.properties")
        file.outputStream().use { props.store(it, null) }
        main(arrayOf("--persistence-dir", dir.absolutePath, "--max-runtime-seconds", "5"))
    }

    @Test
    fun main_persistenceDirWithRunningWork_healsToEnqueued(@TempDir tmp: Path) {
        val dir = tmp.toFile()
        val props = Properties()
        props.setProperty("state", "RUNNING")
        val file = File(dir, "work-002.properties")
        file.outputStream().use { props.store(it, null) }
        main(arrayOf("--persistence-dir", dir.absolutePath, "--max-runtime-seconds", "5"))
        val loaded = Properties()
        file.inputStream().use { loaded.load(it) }
        val healedState = loaded.getProperty("state")
        assert(healedState == "ENQUEUED") { "RUNNING should be healed to ENQUEUED, was: $healedState" }
    }

    @Test
    fun main_persistenceDirMissing_doesNotThrow() {
        main(
            arrayOf(
                "--persistence-dir",
                "/tmp/worker-kmp-test-nonexistent-${System.currentTimeMillis()}",
                "--max-runtime-seconds",
                "1",
            ),
        )
    }
}
