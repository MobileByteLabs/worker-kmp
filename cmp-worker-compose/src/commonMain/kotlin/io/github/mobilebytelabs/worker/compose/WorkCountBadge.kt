package io.github.mobilebytelabs.worker.compose

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.mobilebytelabs.worker.WorkManager

/**
 * Overlays a numeric badge on top of [content] showing the count of active (non-terminal)
 * work items with [tag].
 *
 * The badge disappears automatically when the count reaches zero. Subscribes to the live
 * flow from [LocalWorkManager.current].
 *
 * Requires a [WorkManagerProvider] ancestor in the composition tree.
 *
 * ```kotlin
 * WorkCountBadge(tag = "upload") {
 *     Icon(Icons.Default.Upload, contentDescription = "Uploads")
 * }
 * ```
 *
 * @param tag the work tag whose active items are counted.
 * @param modifier optional [Modifier] applied to the [BadgedBox].
 * @param content the composable beneath the badge (typically an icon or button).
 */
@Composable
fun WorkCountBadge(tag: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val activeCount by rememberActiveWorkCount(tag)

    BadgedBox(
        modifier = modifier,
        badge = {
            if (activeCount > 0) {
                Badge { Text(text = activeCount.toString()) }
            }
        },
    ) {
        content()
    }
}

/**
 * Returns the count of active (non-terminal) work items with [tag] as Compose [State].
 *
 * Derives from [WorkManager.collectWorkInfosByTagAsState] — updates reactively as work
 * items transition between states.
 *
 * Requires a [WorkManagerProvider] ancestor in the composition tree.
 */
@Composable
fun rememberActiveWorkCount(tag: String): State<Int> {
    val workManager = rememberWorkManager()
    val items by workManager.collectWorkInfosByTagAsState(tag)
    return remember(items) { derivedStateOf { items.count { !it.isFinished } } }
}
