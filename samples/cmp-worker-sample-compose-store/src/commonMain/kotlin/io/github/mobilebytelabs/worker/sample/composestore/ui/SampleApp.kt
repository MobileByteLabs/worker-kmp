package io.github.mobilebytelabs.worker.sample.composestore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.app.WorkerKmpAppContent
import io.github.mobilebytelabs.worker.compose.BackgroundCapabilitiesBanner
import io.github.mobilebytelabs.worker.compose.WorkManagerProvider
import io.github.mobilebytelabs.worker.sample.composestore.domain.Article
import org.koin.compose.koinInject
import org.mobilenativefoundation.store.store5.Store

/**
 * Sample app entry composable — fully commonMain, fully target-agnostic.
 *
 * Takes no parameters: dependencies (the platform-specific [WorkManager] + the shared
 * Store5 [Store]) are pulled from Koin via [koinInject]. Per-platform launcher files
 * (`MainActivity` on Android, `Main.kt` on JVM Desktop, `MainViewController` on iOS,
 * `Main.kt` on wasmJs) only need to call this composable — the worker-kmp library
 * handles Koin start + Compose entry point binding through the
 * `cmp-worker-{android,desktop,ios,web}` launcher helpers.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md` AC11.
 */
@WorkerKmpAppContent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp() {
    val workManager: WorkManager = koinInject()
    val store: Store<String, Article> = koinInject()

    MaterialTheme {
        WorkManagerProvider(workManager = workManager) {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("worker-kmp × Store5 × Koin × Compose") })
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BackgroundCapabilitiesBanner(modifier = Modifier.fillMaxWidth())

                    Text(
                        text = "End-to-end demo: a worker drives a Store5 fetch, " +
                            "the cache feeds the UI, and Koin wires it all together.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ArticlesScreen(store = store)
                }
            }
        }
    }
}
