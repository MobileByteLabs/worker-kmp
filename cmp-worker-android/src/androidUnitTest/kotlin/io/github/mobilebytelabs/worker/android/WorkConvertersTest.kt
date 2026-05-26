package io.github.mobilebytelabs.worker.android

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.workDataOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class WorkConvertersTest {

    // ── UUID tag helpers ──────────────────────────────────────────────────────

    @Test
    fun toTag_producesKmpIdPrefixedString() {
        val id = Uuid.parse("550e8400-e29b-41d4-a716-446655440000")
        assertEquals("kmp_id:550e8400-e29b-41d4-a716-446655440000", id.toTag())
    }

    @Test
    fun extractKmpId_parsesTagCorrectly() {
        val id = Uuid.random()
        val tags = setOf(id.toTag(), "user-tag")
        assertEquals(id, tags.extractKmpId())
    }

    @Test
    fun extractKmpId_returnsNullWhenNoKmpTag() {
        assertNull(setOf("user-tag", "another-tag").extractKmpId())
    }

    @Test
    fun userTags_filtersOutKmpIdTags() {
        val id = Uuid.random()
        val tags = setOf(id.toTag(), "sync", "batch")
        val user = tags.userTags()
        assertEquals(setOf("sync", "batch"), user)
    }

    // ── WorkData ↔ Android Data ───────────────────────────────────────────────

    @Test
    fun workData_roundtripExcludesInternalKeys() {
        val data = workDataOf(
            "key1" to "hello",
            "key2" to 42,
            "key3" to true,
        )
        val androidData = data.toAndroid()
        val backAgain = androidData.toKmp()
        assertEquals("hello", backAgain.getString("key1"))
        assertEquals(42, backAgain.getInt("key2"))
        assertEquals(true, backAgain.getBoolean("key3"))
    }

    @Test
    fun toKmp_stripsKmpInternalKeys() {
        val androidData = androidx.work.workDataOf(
            KEY_KMP_CLASS to "com.example.MyWorker",
            KEY_KMP_ID to "550e8400-e29b-41d4-a716-446655440000",
            "user_key" to "user_value",
        )
        val kmpData = androidData.toKmp()
        assertNull(kmpData.getString(KEY_KMP_CLASS))
        assertNull(kmpData.getString(KEY_KMP_ID))
        assertEquals("user_value", kmpData.getString("user_key"))
    }

    @Test
    fun toAndroid_emptyWorkData_producesEmptyAndroidData() {
        val androidData = WorkData.EMPTY.toAndroid()
        assertTrue(androidData.keyValueMap.isEmpty())
    }

    // ── ContentUriTrigger ─────────────────────────────────────────────────────

    @Test
    fun contentUriTrigger_appearsInAndroidConstraints() {
        val constraints = Constraints {
            addContentUriTrigger("content://com.example/items", triggerForDescendants = true)
            addContentUriTrigger("content://com.example/settings", triggerForDescendants = false)
        }
        val android = constraints.toAndroid()
        val triggers = android.contentUriTriggers
        assertEquals(2, triggers.size)
        assertTrue(triggers.any { it.uri.toString() == "content://com.example/items" && it.isTriggeredForDescendants })
        assertTrue(
            triggers.any {
                it.uri.toString() == "content://com.example/settings" && !it.isTriggeredForDescendants
            },
        )
    }

    @Test
    fun noContentUriTriggers_producesEmptySet() {
        val android = Constraints.NONE.toAndroid()
        assertTrue(android.contentUriTriggers.isEmpty())
    }
}
