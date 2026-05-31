package io.github.mobilebytelabs.worker.scheduler.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

/**
 * Reads/writes ChangeListVersions. Phase 5 will wire this to the library's DataStore abstraction;
 * Phase 3 ships a MutableStateFlow-backed in-memory impl so adopters can be unit-tested without
 * depending on the platform DataStore actual.
 *
 * D9: serializes ChangeListVersions as kotlinx.serialization JSON.
 */
class SyncStatePersister(private val json: Json = Json { ignoreUnknownKeys = true }) {
    private val state = MutableStateFlow(ChangeListVersions())

    suspend fun read(): ChangeListVersions = state.value

    suspend fun write(versions: ChangeListVersions) {
        state.value = versions
    }
}
