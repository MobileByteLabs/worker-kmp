package io.github.mobilebytelabs.worker.daemon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class HmacPersistenceTest {

    @Test
    fun writeStamped_thenReadVerified_roundTrips(@TempDir tmp: Path) {
        val hp = HmacPersistence(tmp.toFile().absolutePath)
        val lines = listOf("state=ENQUEUED", "tags=daily-sync")
        val file = File(tmp.toFile(), "work.properties")
        hp.writeStamped(file, lines)
        val result = hp.readVerified(file)
        assertNotNull(result, "readVerified should succeed after writeStamped")
        assertEquals(lines, result)
    }

    @Test
    fun readVerified_tamperedContent_returnsNull(@TempDir tmp: Path) {
        val hp = HmacPersistence(tmp.toFile().absolutePath)
        val file = File(tmp.toFile(), "work.properties")
        hp.writeStamped(file, listOf("state=ENQUEUED"))
        val original = file.readText()
        // Tamper: replace ENQUEUED with SUCCEEDED
        file.writeText(original.replace("ENQUEUED", "SUCCEEDED"))
        assertNull(hp.readVerified(file), "tampered file should be rejected")
    }

    @Test
    fun readVerified_missingHmacStamp_returnsNull(@TempDir tmp: Path) {
        val hp = HmacPersistence(tmp.toFile().absolutePath)
        val file = File(tmp.toFile(), "work.properties")
        file.writeText("state=ENQUEUED\n")
        assertNull(hp.readVerified(file), "file without HMAC stamp should be rejected")
    }

    @Test
    fun readVerified_nonExistentFile_returnsNull(@TempDir tmp: Path) {
        val hp = HmacPersistence(tmp.toFile().absolutePath)
        val file = File(tmp.toFile(), "does-not-exist.properties")
        assertNull(hp.readVerified(file))
    }

    @Test
    fun writeStamped_fileContainsHmacPrefix(@TempDir tmp: Path) {
        val hp = HmacPersistence(tmp.toFile().absolutePath)
        val file = File(tmp.toFile(), "stamped.properties")
        hp.writeStamped(file, listOf("state=SUCCEEDED"))
        assert(file.readText().startsWith(HmacPersistence.HMAC_PREFIX)) {
            "written file must begin with HMAC prefix"
        }
    }

    @Test
    fun keyFile_isCreatedInPersistenceDir(@TempDir tmp: Path) {
        val hp = HmacPersistence(tmp.toFile().absolutePath)
        val file = File(tmp.toFile(), "key-test.properties")
        hp.writeStamped(file, listOf("state=ENQUEUED"))
        assert(File(tmp.toFile(), ".persistence-hmac.key").exists()) {
            "HMAC key file should be created in persistenceDir"
        }
    }
}
