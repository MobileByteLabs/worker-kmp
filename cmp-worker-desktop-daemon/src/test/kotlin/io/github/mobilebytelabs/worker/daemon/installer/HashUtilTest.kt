package io.github.mobilebytelabs.worker.daemon.installer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class HashUtilTest {

    @Test
    fun sha256Hex_knownContent_returnsExpectedDigest(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "test.bin")
        file.writeBytes(ByteArray(0))
        // SHA-256 of empty byte array is known
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(expected, HashUtil.sha256Hex(file))
    }

    @Test
    fun sha256Hex_nonEmptyFile_returnsHexString(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "data.bin")
        file.writeText("hello world")
        val result = HashUtil.sha256Hex(file)
        assertNotNull(result)
        assert(result!!.length == 64) { "SHA-256 hex should be 64 characters, was ${result.length}" }
        assert(result.all { it.isDigit() || it in 'a'..'f' }) { "hex digest must contain only lowercase hex chars" }
    }

    @Test
    fun sha256Hex_missingFile_returnsNull(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "does-not-exist.bin")
        assertNull(HashUtil.sha256Hex(file))
    }

    @Test
    fun sha256Hex_sameContentDifferentFiles_sameDigest(@TempDir tmp: Path) {
        val content = "worker-kmp daemon test content"
        val file1 = File(tmp.toFile(), "f1.txt")
        val file2 = File(tmp.toFile(), "f2.txt")
        file1.writeText(content)
        file2.writeText(content)
        assertEquals(HashUtil.sha256Hex(file1), HashUtil.sha256Hex(file2))
    }

    @Test
    fun sha256Hex_differentContent_differentDigests(@TempDir tmp: Path) {
        val file1 = File(tmp.toFile(), "a.txt")
        val file2 = File(tmp.toFile(), "b.txt")
        file1.writeText("content A")
        file2.writeText("content B")
        assert(HashUtil.sha256Hex(file1) != HashUtil.sha256Hex(file2)) {
            "different content should produce different digests"
        }
    }
}
