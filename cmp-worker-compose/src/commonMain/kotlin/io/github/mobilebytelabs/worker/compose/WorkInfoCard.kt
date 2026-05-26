package io.github.mobilebytelabs.worker.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo

/**
 * Material Design 3 card that displays a full snapshot of a [WorkInfo] instance.
 *
 * Shows:
 * - Shortened work ID and [WorkStatusChip]
 * - [WorkProgressIndicator] when the work is running or has progress to report
 * - Output data key-value pairs when the work has finished with data
 * - Cancel button while the work is active; Retry button when [WorkInfo.State.FAILED]
 *
 * ```kotlin
 * WorkInfoCard(
 *     info = workInfo,
 *     onCancel = { workManager.cancelWorkById(workInfo.id) },
 *     onRetry  = { workManager.enqueue(buildRetryRequest(workInfo)) },
 * )
 * ```
 *
 * @param info the [WorkInfo] to display.
 * @param onCancel invoked when the user taps Cancel; called only when work is not finished.
 * @param onRetry invoked when the user taps Retry; called only when state is [WorkInfo.State.FAILED].
 * @param modifier optional [Modifier] applied to the outer [Card].
 */
@Composable
fun WorkInfoCard(
    info: WorkInfo,
    onCancel: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = info.id.toString().take(8) + "…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WorkStatusChip(state = info.state)
            }

            if (info.state == WorkInfo.State.RUNNING || !info.progress.isIndeterminate) {
                WorkProgressIndicator(progress = info.progress)
            }

            if (info.outputData != WorkData.EMPTY) {
                OutputDataSection(data = info.outputData)
            }

            if (info.runAttemptCount > 1) {
                Text(
                    text = "Attempt ${info.runAttemptCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val showCancel = !info.isFinished && onCancel != null
            val showRetry = info.state == WorkInfo.State.FAILED && onRetry != null
            if (showCancel || showRetry) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (showRetry) {
                        TextButton(onClick = onRetry!!) {
                            Text("Retry")
                        }
                    }
                    if (showCancel) {
                        TextButton(
                            onClick = onCancel!!,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputDataSection(data: WorkData) {
    val entries = data.keyValueMap().entries.toList()
    if (entries.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Output",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entries.forEach { (key, value) ->
            Text(
                text = "$key: $value",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
