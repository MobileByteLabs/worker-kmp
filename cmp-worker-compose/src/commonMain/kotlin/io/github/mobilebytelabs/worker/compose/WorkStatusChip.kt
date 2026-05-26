package io.github.mobilebytelabs.worker.compose

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.mobilebytelabs.worker.WorkInfo

/**
 * Material Design 3 chip that visualises a [WorkInfo.State] with a state-specific
 * label, icon, and colour.
 *
 * | State      | Colour  |
 * |------------|---------|
 * | ENQUEUED   | outline |
 * | RUNNING    | primary |
 * | SUCCEEDED  | tertiary |
 * | FAILED     | error   |
 * | CANCELLED  | outline |
 * | BLOCKED    | secondary |
 *
 * ```kotlin
 * WorkStatusChip(state = info.state)
 * ```
 */
@Composable
fun WorkStatusChip(state: WorkInfo.State, modifier: Modifier = Modifier) {
    val label = state.label()
    val containerColor = state.containerColor()
    val labelColor = state.labelColor()

    AssistChip(
        onClick = {},
        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            val icon = state.icon()
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                    tint = labelColor,
                )
            }
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconContentColor = labelColor,
        ),
    )
}

@Composable
private fun WorkInfo.State.containerColor(): Color = when (this) {
    WorkInfo.State.RUNNING -> MaterialTheme.colorScheme.primaryContainer

    WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.tertiaryContainer

    WorkInfo.State.FAILED -> MaterialTheme.colorScheme.errorContainer

    WorkInfo.State.ENQUEUED,
    WorkInfo.State.CANCELLED,
    WorkInfo.State.BLOCKED,
    -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun WorkInfo.State.labelColor(): Color = when (this) {
    WorkInfo.State.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer

    WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.onTertiaryContainer

    WorkInfo.State.FAILED -> MaterialTheme.colorScheme.onErrorContainer

    WorkInfo.State.ENQUEUED,
    WorkInfo.State.CANCELLED,
    WorkInfo.State.BLOCKED,
    -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun WorkInfo.State.label(): String = when (this) {
    WorkInfo.State.ENQUEUED -> "Enqueued"
    WorkInfo.State.RUNNING -> "Running"
    WorkInfo.State.SUCCEEDED -> "Succeeded"
    WorkInfo.State.FAILED -> "Failed"
    WorkInfo.State.CANCELLED -> "Cancelled"
    WorkInfo.State.BLOCKED -> "Blocked"
}

private fun WorkInfo.State.icon(): ImageVector? = null
