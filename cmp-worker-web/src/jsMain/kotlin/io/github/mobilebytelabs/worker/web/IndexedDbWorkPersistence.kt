package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.await
import kotlin.js.Promise
import kotlin.uuid.Uuid

private const val STORE_NAME = "work_infos"

// Factory function: creates a self-contained IDB helper bound to the given dbName.
// Using a function (not a top-level val) lets callers pass dbName as a JS argument,
// avoiding Kotlin variable capture issues inside js("") string literals.
@Suppress("UnsafeCastFromDynamic")
private fun buildIdbHelper(dbName: String): dynamic = js(
    """(function(dbName) { return {
    _db: null,
    getDb: function() {
        var self = this;
        if (self._db) return Promise.resolve(self._db);
        return new Promise(function(resolve, reject) {
            var req = indexedDB.open(dbName, 1);
            req.onupgradeneeded = function(e) {
                e.target.result.createObjectStore("work_infos", { keyPath: "id" });
            };
            req.onsuccess = function(e) {
                self._db = e.target.result;
                resolve(self._db);
            };
            req.onerror = function(e) { reject(e.target.error); };
        });
    },
    put: function(record) {
        var self = this;
        return self.getDb().then(function(db) {
            return new Promise(function(resolve, reject) {
                var tx = db.transaction("work_infos", "readwrite");
                var req = tx.objectStore("work_infos").put(record);
                req.onsuccess = function() { resolve(); };
                req.onerror = function(e) { reject(e.target.error); };
            });
        });
    },
    getAll: function() {
        var self = this;
        return self.getDb().then(function(db) {
            return new Promise(function(resolve, reject) {
                var tx = db.transaction("work_infos", "readonly");
                var req = tx.objectStore("work_infos").getAll();
                req.onsuccess = function(e) { resolve(e.target.result); };
                req.onerror = function(e) { reject(e.target.error); };
            });
        });
    },
    remove: function(id) {
        var self = this;
        return self.getDb().then(function(db) {
            return new Promise(function(resolve, reject) {
                var tx = db.transaction("work_infos", "readwrite");
                var req = tx.objectStore("work_infos").delete(id);
                req.onsuccess = function() { resolve(); };
                req.onerror = function(e) { reject(e.target.error); };
            });
        });
    }
};})(dbName)""",
)(dbName)

// Guard for private browsing / SSR / old browsers where indexedDB is unavailable.
@Suppress("UnsafeCastFromDynamic")
private fun isIndexedDbAvailable(): Boolean = js("typeof indexedDB !== 'undefined' && indexedDB !== null") as Boolean

private fun workInfoToRecord(info: WorkInfo): dynamic {
    val record: dynamic = js("({})")
    record["id"] = info.id.toString()
    record["state"] = info.state.name
    record["runAttemptCount"] = info.runAttemptCount
    val tagsArray: dynamic = js("([])")
    info.tags.forEachIndexed { i, tag -> tagsArray[i] = tag }
    record["tags"] = tagsArray
    return record
}

@Suppress("UnsafeCastFromDynamic")
private fun recordToWorkInfo(record: dynamic): WorkInfo? {
    val idStr = record["id"] as? String ?: return null
    val stateStr = record["state"] as? String ?: return null
    val state = WorkInfo.State.entries.firstOrNull { it.name == stateStr } ?: return null
    val attemptCount = (record["runAttemptCount"] as? Int) ?: 0
    val tagsArray = record["tags"]
    val tags = mutableSetOf<String>()
    val len = (tagsArray?.length as? Int) ?: 0
    for (i in 0 until len) {
        (tagsArray[i] as? String)?.let { tags.add(it) }
    }
    return WorkInfo(
        id = Uuid.parse(idStr),
        state = state,
        tags = tags,
        runAttemptCount = attemptCount,
    )
}

internal class IndexedDbWorkPersistence(dbName: String) : WebWorkPersistence {
    private val idb: dynamic = buildIdbHelper(dbName)

    override suspend fun save(info: WorkInfo) {
        if (!isIndexedDbAvailable()) return
        try {
            (idb.put(workInfoToRecord(info)) as Promise<Unit>).await()
        } catch (_: Exception) {
            // Best-effort — in-memory is authoritative
        }
    }

    override suspend fun loadAll(): List<WorkInfo> {
        if (!isIndexedDbAvailable()) return emptyList()
        return try {
            val records = (idb.getAll() as Promise<dynamic>).await()
            val len = (records.length as? Int) ?: 0
            (0 until len).mapNotNull { i -> recordToWorkInfo(records[i]) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun delete(id: Uuid) {
        if (!isIndexedDbAvailable()) return
        try {
            (idb.remove(id.toString()) as Promise<Unit>).await()
        } catch (_: Exception) {
            // Best-effort
        }
    }
}

private object NoOpPersistence : WebWorkPersistence {
    override suspend fun save(info: WorkInfo) = Unit
    override suspend fun loadAll(): List<WorkInfo> = emptyList()
    override suspend fun delete(id: Uuid) = Unit
}

internal actual fun createWebWorkPersistence(config: WebWorkManagerConfig): WebWorkPersistence =
    if (config.enablePersistence && isIndexedDbAvailable()) {
        IndexedDbWorkPersistence(config.persistenceDbName)
    } else {
        NoOpPersistence
    }
