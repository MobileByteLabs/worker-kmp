package io.github.mobilebytelabs.worker.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.WorkProgress

/**
 * Linear progress bar bound to a [WorkProgress] value.
 *
 * Shows an indeterminate bar when [progress] is [WorkProgress.NONE] (`progress == 0`),
 * or a determinate bar filled to the reported percentage otherwise.
 * An optional [statusMessage] is rendered below the bar.
 *
 * ```kotlin
 * WorkProgressIndicator(progress = info.progress)
 * WorkProgressIndicator(progress = info.progress, statusMessage = "Uploading…")
 * ```
 *
 * @param progress the [WorkProgress] value to visualise.
 * @param statusMessage optional text rendered below the progress bar.
 * @param modifier optional [Modifier] applied to the outer [Column].
 */
@Composable
fun WorkProgressIndicator(progress: WorkProgress, statusMessage: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (progress.isIndeterminate) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                progress = { progress.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val label = when {
            statusMessage != null -> statusMessage
            progress.isIndeterminate -> "In progress…"
            progress.isComplete -> "Complete"
            else -> "${progress.progress}%"
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
