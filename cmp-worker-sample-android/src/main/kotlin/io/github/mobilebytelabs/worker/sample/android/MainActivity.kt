@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class, io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package io.github.mobilebytelabs.worker.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PlatformWorkManager
import io.github.mobilebytelabs.worker.compose.BackgroundCapabilitiesBanner
import io.github.mobilebytelabs.worker.compose.WorkMonitorScreen
import io.github.mobilebytelabs.worker.compose.WorkSchedulerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkerSampleScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerSampleScreen() {
    val workManager = remember { PlatformWorkManager() }
    val selectedTab = remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("worker-kmp Android Sample") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            BackgroundCapabilitiesBanner(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab.value) {
                listOf("Schedule", "Monitor").forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab.value == i,
                        onClick = { selectedTab.value = i },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab.value) {
                    0 -> WorkSchedulerScreen(
                        onSchedule = { request ->
                            coroutineScope.launch {
                                workManager.enqueue(request as OneTimeWorkRequest)
                            }
                        }
                    )
                    1 -> WorkMonitorScreen(tag = "sample-work")
                }
            }
        }
    }
}
