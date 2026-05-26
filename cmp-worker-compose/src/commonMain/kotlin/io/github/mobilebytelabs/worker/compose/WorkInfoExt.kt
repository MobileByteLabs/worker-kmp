package io.github.mobilebytelabs.worker.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import kotlin.uuid.Uuid

/**
 * Subscribes to work items matching [tag] and returns their list as Compose [State].
 *
 * The state updates whenever the underlying [WorkManager.getWorkInfosByTag] flow emits.
 * Starts with [initial] (empty list by default) on the first composition.
 *
 * ```kotlin
 * val uploads by LocalWorkManager.current.collectWorkInfosByTagAsState("upload")
 * ```
 */
@Composable
fun WorkManager.collectWorkInfosByTagAsState(
    tag: String,
    initial: List<WorkInfo> = emptyList()
): State<List<WorkInfo>> = getWorkInfosByTag(tag).collectAsState(initial = initial)

/**
 * Returns a single [WorkInfo] snapshot for [id] as Compose [State].
 *
 * Performs a one-shot lookup via [WorkManager.getWorkInfoById] when [id] changes.
 * Starts with [initial] (null by default) until the first result arrives.
 *
 * For live updates use [collectWorkInfosByTagAsState] with the same tag you gave the request.
 *
 * ```kotlin
 * val info by LocalWorkManager.current.collectWorkInfoByIdAsState(workId)
 * ```
 */
@Composable
fun WorkManager.collectWorkInfoByIdAsState(
    id: Uuid,
    initial: WorkInfo? = null
): State<WorkInfo?> {
    val state = remember(id) { mutableStateOf(initial) }
    LaunchedEffect(id) {
        state.value = getWorkInfoById(id)
    }
    return state
}
