package io.github.mobilebytelabs.worker.sample

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.WorkRequest
import io.github.mobilebytelabs.worker.compose.BackgroundCapabilitiesBanner
import io.github.mobilebytelabs.worker.compose.WorkMonitorScreen
import io.github.mobilebytelabs.worker.compose.WorkSchedulerScreen
import io.github.mobilebytelabs.worker.desktop.DesktopWorkManager
import io.github.mobilebytelabs.worker.desktop.DesktopWorkManagerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "worker-kmp Compose Desktop Demo"
    ) {
        ComposeSampleApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeSampleApp() {
    val workManager = remember {
        DesktopWorkManager(
            config = DesktopWorkManagerConfig(
                persistencePath = File(System.getProperty("user.home")).resolve(".worker-sample")
            ),
            workerFactory = SampleWorkerFactory,
        )
    }

    val selectedTab = remember { mutableStateOf(0) }
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("worker-kmp Compose Desktop Demo") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            BackgroundCapabilitiesBanner(
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TabBar(selectedTab.value) { selectedTab.value = it }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab.value) {
                    0 -> WorkSchedulerScreen(
                        onSchedule = { workRequest ->
                            coroutineScope.launch {
                                runCatching {
                                    workManager.enqueue(workRequest as OneTimeWorkRequest)
                                }
                            }
                        }
                    )
                    1 -> WorkMonitorScreen(tag = "sample-work")
                }
            }
        }
    }
}

@Composable
private fun TabBar(selected: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Schedule Work", "Monitor Work")
    TabRow(selectedTabIndex = selected) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selected == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

@Preview
@Composable
fun PreviewComposeSampleApp() {
    ComposeSampleApp()
}
