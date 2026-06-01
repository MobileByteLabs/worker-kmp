package io.github.mobilebytelabs.worker.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.NetworkType
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.WorkRequest
import kotlin.time.Duration.Companion.minutes

/**
 * Full-screen form for scheduling one-time or periodic work.
 *
 * The user fills in the worker class name, an optional tag, the work type, and constraints.
 * When the Schedule button is tapped, the built [WorkRequest] is passed to [onSchedule] and
 * the form is reset to its initial state.
 *
 * Requires a [WorkManagerProvider] ancestor in the composition tree.
 *
 * ```kotlin
 * WorkSchedulerScreen(
 *     onSchedule = { request -> LocalWorkManager.current.enqueue(request) }
 * )
 * ```
 *
 * @param onSchedule callback invoked with the fully-built [WorkRequest] when the user confirms.
 * @param modifier optional [Modifier] applied to the root [Column].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkSchedulerScreen(onSchedule: (WorkRequest) -> Unit, modifier: Modifier = Modifier) {
    var workerClass by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var isPeriodic by remember { mutableStateOf(false) }
    var repeatMinutes by remember { mutableStateOf("15") }

    var networkExpanded by remember { mutableStateOf(false) }
    var selectedNetwork by remember { mutableStateOf(NetworkType.NOT_REQUIRED) }
    var requiresCharging by remember { mutableStateOf(false) }
    var requiresBatteryNotLow by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Schedule Work",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = workerClass,
            onValueChange = { workerClass = it },
            label = { Text("Worker class") },
            placeholder = { Text("com.example.SyncWorker") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = tag,
            onValueChange = { tag = it },
            label = { Text("Tag (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { isPeriodic = false },
                selected = !isPeriodic,
                label = { Text("One-time") },
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { isPeriodic = true },
                selected = isPeriodic,
                label = { Text("Periodic") },
            )
        }

        if (isPeriodic) {
            OutlinedTextField(
                value = repeatMinutes,
                onValueChange = { repeatMinutes = it.filter { c -> c.isDigit() } },
                label = { Text("Repeat interval (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        Text(
            text = "Constraints",
            style = MaterialTheme.typography.titleSmall,
        )

        ExposedDropdownMenuBox(
            expanded = networkExpanded,
            onExpandedChange = { networkExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedNetwork.name.replace('_', ' '),
                onValueChange = {},
                readOnly = true,
                label = { Text("Network type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = networkExpanded,
                onDismissRequest = { networkExpanded = false },
            ) {
                NetworkType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name.replace('_', ' ')) },
                        onClick = {
                            selectedNetwork = type
                            networkExpanded = false
                        },
                    )
                }
            }
        }

        ConstraintCheckbox(
            checked = requiresCharging,
            onCheckedChange = { requiresCharging = it },
            label = "Requires charging",
        )

        ConstraintCheckbox(
            checked = requiresBatteryNotLow,
            onCheckedChange = { requiresBatteryNotLow = it },
            label = "Requires battery not low",
        )

        Spacer(modifier = Modifier.height(4.dp))

        val isValid = workerClass.isNotBlank()
        Button(
            onClick = {
                if (!isValid) return@Button
                val constraints = Constraints {
                    setRequiredNetworkType(selectedNetwork)
                    setRequiresCharging(requiresCharging)
                    setRequiresBatteryNotLow(requiresBatteryNotLow)
                }
                val request: WorkRequest = if (isPeriodic) {
                    val interval = (repeatMinutes.toIntOrNull() ?: 15).minutes
                    buildPeriodicRequest(workerClass, tag.trim(), constraints, interval)
                } else {
                    buildOneTimeRequest(workerClass, tag.trim(), constraints)
                }
                onSchedule(request)
                workerClass = ""
                tag = ""
                isPeriodic = false
                repeatMinutes = "15"
                selectedNetwork = NetworkType.NOT_REQUIRED
                requiresCharging = false
                requiresBatteryNotLow = false
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Schedule")
        }
    }
}

@Composable
private fun ConstraintCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

// Visibility raised from `private` → `internal` so commonTest can verify the
// builder branches directly. The enclosing `WorkSchedulerScreen` Composable
// remains the only public-API user. Per kover-100-coverage Phase 6.
internal fun buildOneTimeRequest(workerClass: String, tag: String, constraints: Constraints): OneTimeWorkRequest {
    val builder = OneTimeWorkRequestBuilder<Nothing>(workerClass).setConstraints(constraints)
    if (tag.isNotEmpty()) builder.addTag(tag)
    return builder.build()
}

internal fun buildPeriodicRequest(
    workerClass: String,
    tag: String,
    constraints: Constraints,
    interval: kotlin.time.Duration,
): PeriodicWorkRequest {
    val builder = PeriodicWorkRequestBuilder<Nothing>(workerClass, interval).setConstraints(constraints)
    if (tag.isNotEmpty()) builder.addTag(tag)
    return builder.build()
}
