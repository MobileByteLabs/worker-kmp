package io.github.mobilebytelabs.worker.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.WorkInfo

/**
 * Full-screen monitor that lists all [WorkInfo] items matching [tag] as [WorkInfoCard]s.
 *
 * Subscribes to the live flow from [LocalWorkManager.current] so cards update automatically
 * as work states change. Shows a placeholder message when no items are found.
 *
 * Requires a [WorkManagerProvider] ancestor in the composition tree.
 *
 * ```kotlin
 * WorkMonitorScreen(tag = "upload")
 *
 * // With cancel/retry callbacks:
 * WorkMonitorScreen(
 *     tag = "sync",
 *     onCancel = { id -> workManager.cancelWorkById(id) },
 *     onRetry  = { info -> workManager.enqueue(buildRetryRequest(info)) },
 * )
 * ```
 *
 * @param tag the tag whose work items are displayed.
 * @param emptyMessage text shown when no work items match [tag].
 * @param onCancel optional callback invoked with the [WorkInfo] when the user taps Cancel.
 * @param onRetry optional callback invoked with the [WorkInfo] when the user taps Retry.
 * @param modifier optional [Modifier] applied to the root composable.
 */
@Composable
fun WorkMonitorScreen(
    tag: String,
    emptyMessage: String = "No active work for tag \"$tag\"",
    onCancel: ((WorkInfo) -> Unit)? = null,
    onRetry: ((WorkInfo) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val workManager = rememberWorkManager()
    val workItems by workManager.collectWorkInfosByTagAsState(tag)
    val cancelCb = onCancel
    val retryCb = onRetry

    if (workItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(workItems, key = { it.id.toString() }) { info ->
                WorkInfoCard(
                    info = info,
                    onCancel = if (cancelCb != null) {
                        { cancelCb(info) }
                    } else {
                        null
                    },
                    onRetry = if (retryCb != null) {
                        { retryCb(info) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
