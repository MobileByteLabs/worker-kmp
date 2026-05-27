package io.github.mobilebytelabs.worker.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.BackgroundCapabilities
import io.github.mobilebytelabs.worker.platformBackgroundCapabilities

/**
 * Informational banner that describes the background capabilities active on the current platform.
 *
 * Shows two rows — "Persistence" and "OS scheduling" — each with a ✓ or – indicator.
 * Useful in app settings screens or debug panels.
 *
 * Hidden by default when both capabilities are unavailable. Pass `forceVisible = true`
 * to always render it (e.g. for testing or documentation).
 *
 * ```kotlin
 * BackgroundCapabilitiesBanner()
 *
 * // With explicit capabilities (e.g. in tests / previews):
 * BackgroundCapabilitiesBanner(
 *     capabilities = BackgroundCapabilities(supportsPersistence = true, supportsOsScheduling = false),
 * )
 * ```
 *
 * @param capabilities the capabilities to display; defaults to [platformBackgroundCapabilities].
 * @param forceVisible when `true`, renders the banner even when all capabilities are unavailable.
 * @param modifier optional [Modifier].
 */
@Composable
fun BackgroundCapabilitiesBanner(
    capabilities: BackgroundCapabilities = remember { platformBackgroundCapabilities() },
    forceVisible: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!forceVisible && !capabilities.supportsPersistence && !capabilities.supportsOsScheduling) return

    val hasOsScheduling = capabilities.supportsOsScheduling

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (hasOsScheduling) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Background Mode",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasOsScheduling) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            CapabilityRow(label = "Persistence", enabled = capabilities.supportsPersistence)
            CapabilityRow(label = "OS scheduling", enabled = capabilities.supportsOsScheduling)
        }
    }
}

@Composable
private fun CapabilityRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (enabled) "✓" else "–",
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
