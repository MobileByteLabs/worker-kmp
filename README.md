# WorkKit KMP ⚡

[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-blue)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-green)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/workkit-kmp)](https://search.maven.org/artifact/io.github.mobilebytelabs/workkit-kmp)

A powerful, cross-platform background task scheduler built with Kotlin Multiplatform. Schedule, manage, and monitor background work across Android, iOS, and Desktop platforms with a unified API following Clean Architecture principles.

## ✨ Features

- 🚀 **Cross-Platform Support**: Android (WorkManager), iOS (Background Tasks), Desktop (Coroutines)
- ⏰ **Flexible Scheduling**: One-time, periodic, and delayed task execution
- 🔄 **Retry Logic**: Configurable retry policies with exponential backoff
- 🏗️ **Clean Architecture**: SOLID principles with dependency injection support
- 📊 **Work Monitoring**: Real-time work status tracking and observability
- 🔧 **Constraint-Based**: Network, battery, storage, and charging constraints
- 🎯 **Type-Safe**: Kotlin coroutines with structured concurrency
- 📱 **Compose Integration**: UI components for work status monitoring
- 🌙 **Material Design 3**: Full theming support including dark mode
- 🧪 **Testing Ready**: Comprehensive testing utilities and mocks

## 🚀 Platform Support

| Platform | Implementation | Min Version | Features |
|----------|---------------|-------------|----------|
| Android  | WorkManager   | API 21+     | ✅ Full feature set |
| iOS      | BGTaskScheduler | iOS 13+   | ✅ Background processing |
| Desktop  | Coroutines    | JVM 11+     | ✅ Foreground processing |

## 📦 Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.mobilebytelabs:workkit-kmp:1.0.0")
    
    // For Compose UI components
    implementation("io.github.mobilebytelabs:workkit-compose:1.0.0")
    
    // Platform-specific implementations (automatically included)
    // implementation("io.github.mobilebytelabs:workkit-android:1.0.0")
    // implementation("io.github.mobilebytelabs:workkit-ios:1.0.0")
    // implementation("io.github.mobilebytelabs:workkit-desktop:1.0.0")
}
```

### Version Catalog

```toml
[versions]
workkit = "1.0.0"

[libraries]
workkit-kmp = { group = "io.github.mobilebytelabs", name = "workkit-kmp", version.ref = "workkit" }
workkit-compose = { group = "io.github.mobilebytelabs", name = "workkit-compose", version.ref = "workkit" }
```

## 🎯 Quick Start

### Basic Work Definition

```kotlin
class DataSyncWorker : CoroutineWorker() {
    
    override suspend fun doWork(
        inputData: WorkData,
        progressCallback: ProgressCallback
    ): WorkResult {
        return try {
            val apiKey = inputData.getString(KEY_API_KEY) ?: return WorkResult.failure()
            val syncType = inputData.getEnum<SyncType>(KEY_SYNC_TYPE) ?: SyncType.INCREMENTAL
            
            progressCallback.setProgress(
                WorkProgress(
                    progress = 0,
                    statusMessage = "Starting sync..."
                )
            )
            
            val syncService = SyncService(apiKey)
            val result = syncService.syncData(
                type = syncType,
                onProgress = { progress ->
                    progressCallback.setProgress(
                        WorkProgress(
                            progress = progress,
                            statusMessage = "Syncing data: ${progress}%"
                        )
                    )
                }
            )
            
            WorkResult.success(
                outputData = workDataOf(
                    KEY_SYNC_COUNT to result.syncedItems,
                    KEY_LAST_SYNC_TIME to System.currentTimeMillis()
                )
            )
            
        } catch (exception: Exception) {
            WorkResult.retry(
                retryReason = exception.message ?: "Unknown error"
            )
        }
    }
    
    companion object {
        const val KEY_API_KEY = "api_key"
        const val KEY_SYNC_TYPE = "sync_type"
        const val KEY_SYNC_COUNT = "sync_count"
        const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }
}
```

### Scheduling Work

```kotlin
@Composable
fun WorkSchedulerScreen(
    workManager: WorkManager = LocalWorkManager.current,
    modifier: Modifier = Modifier
) {
    var isScheduling by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // One-time work
        WorkScheduleCard(
            title = stringResource(R.string.schedule_one_time_work),
            description = stringResource(R.string.schedule_one_time_description),
            isLoading = isScheduling,
            onScheduleClick = {
                isScheduling = true
                scheduleOneTimeWork(workManager) {
                    isScheduling = false
                }
            }
        )
        
        // Periodic work
        WorkScheduleCard(
            title = stringResource(R.string.schedule_periodic_work),
            description = stringResource(R.string.schedule_periodic_description),
            isLoading = isScheduling,
            onScheduleClick = {
                isScheduling = true
                schedulePeriodicWork(workManager) {
                    isScheduling = false
                }
            }
        )
    }
}

private fun scheduleOneTimeWork(
    workManager: WorkManager,
    onComplete: () -> Unit
) {
    val workRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
        .setInputData(
            workDataOf(
                DataSyncWorker.KEY_API_KEY to "your_api_key",
                DataSyncWorker.KEY_SYNC_TYPE to SyncType.FULL.name
            )
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .setBackoffCriteria(
            backoffPolicy = BackoffPolicy.EXPONENTIAL,
            backoffDelay = Duration.ofMinutes(1)
        )
        .build()
    
    workManager.enqueue(workRequest)
    onComplete()
}

private fun schedulePeriodicWork(
    workManager: WorkManager,
    onComplete: () -> Unit
) {
    val periodicRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
        repeatInterval = Duration.ofHours(6),
        flexTimeInterval = Duration.ofHours(1)
    )
        .setInputData(
            workDataOf(
                DataSyncWorker.KEY_API_KEY to "your_api_key",
                DataSyncWorker.KEY_SYNC_TYPE to SyncType.INCREMENTAL.name
            )
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()
    
    workManager.enqueueUniquePeriodicWork(
        uniqueWorkName = "periodic_data_sync",
        existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        periodicWorkRequest = periodicRequest
    )
    
    onComplete()
}
```

### Work Monitoring UI

```kotlin
@Composable
fun WorkMonitorScreen(
    workManager: WorkManager = LocalWorkManager.current,
    modifier: Modifier = Modifier
) {
    val workInfos by workManager.getWorkInfosByTagLiveData("data_sync")
        .observeAsState(emptyList())
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(workInfos) { workInfo ->
            WorkInfoCard(
                workInfo = workInfo,
                onCancelClick = { workManager.cancelWorkById(workInfo.id) },
                onRetryClick = { 
                    // Retry logic implementation
                    retryWork(workManager, workInfo.id)
                }
            )
        }
    }
}

@Composable
fun WorkInfoCard(
    workInfo: WorkInfo,
    onCancelClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardColors: CardColors = CardDefaults.cardColors(),
    cardElevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = cardColors,
        elevation = cardElevation
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.work_id_label, workInfo.id.toString().take(8)),
                    style = MaterialTheme.typography.titleMedium
                )
                
                WorkStatusChip(
                    status = workInfo.state,
                    colors = getStatusChipColors(workInfo.state)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            WorkProgressIndicator(
                progress = workInfo.progress,
                showPercentage = true
            )
            
            if (workInfo.outputData.keyValueMap.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                WorkOutputData(
                    outputData = workInfo.outputData
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (workInfo.state == WorkInfo.State.FAILED) {
                    TextButton(onClick = onRetryClick) {
                        Text(stringResource(R.string.retry_work))
                    }
                }
                
                if (workInfo.state in listOf(
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING
                )) {
                    TextButton(onClick = onCancelClick) {
                        Text(stringResource(R.string.cancel_work))
                    }
                }
            }
        }
    }
}

@Composable
fun WorkStatusChip(
    status: WorkInfo.State,
    modifier: Modifier = Modifier,
    colors: ChipColors = getStatusChipColors(status)
) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = stringResource(getStatusStringRes(status)),
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = getStatusIcon(status),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier,
        colors = colors
    )
}
```

## 🏗️ Architecture

WorkKit KMP follows Clean Architecture principles with clear separation of concerns:

```
┌─────────────────────┐
│    Presentation     │  ← Compose UI Components, ViewModels
├─────────────────────┤
│      Domain         │  ← Use Cases, Entities, Repository Interfaces
├─────────────────────┤
│       Data          │  ← Repository Implementations, Data Sources
├─────────────────────┤
│    Framework        │  ← Platform-specific Work Implementations
└─────────────────────┘
```

### Core Components

#### Work Definition

```kotlin
abstract class CoroutineWorker {
    abstract suspend fun doWork(
        inputData: WorkData,
        progressCallback: ProgressCallback
    ): WorkResult
}

sealed class WorkResult {
    object Success : WorkResult()
    data class Failure(val reason: String? = null) : WorkResult()
    data class Retry(val retryReason: String? = null) : WorkResult()
    
    companion object {
        fun success(outputData: WorkData = WorkData.EMPTY) = Success
        fun failure(reason: String? = null) = Failure(reason)
        fun retry(retryReason: String? = null) = Retry(retryReason)
    }
}
```

#### Work Request Builder

```kotlin
class OneTimeWorkRequestBuilder<T : CoroutineWorker> {
    
    fun setInputData(inputData: WorkData): OneTimeWorkRequestBuilder<T>
    fun setConstraints(constraints: Constraints): OneTimeWorkRequestBuilder<T>
    fun setBackoffCriteria(
        backoffPolicy: BackoffPolicy,
        backoffDelay: Duration
    ): OneTimeWorkRequestBuilder<T>
    fun addTag(tag: String): OneTimeWorkRequestBuilder<T>
    
    fun build(): OneTimeWorkRequest
}

class PeriodicWorkRequestBuilder<T : CoroutineWorker>(
    repeatInterval: Duration,
    flexTimeInterval: Duration = Duration.ZERO
) {
    // Similar methods as OneTimeWorkRequestBuilder
    fun build(): PeriodicWorkRequest
}
```

### Dependency Injection

#### Koin Example

```kotlin
val workKitModule = module {
    single<WorkManager> { PlatformWorkManager() }
    single<WorkRepository> { WorkRepositoryImpl(get()) }
    factory { ScheduleWorkUseCase(get()) }
    factory { MonitorWorkUseCase(get()) }
    factory { CancelWorkUseCase(get()) }
}
```

#### Dagger/Hilt Example

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class WorkKitModule {
    
    @Binds
    abstract fun bindWorkRepository(
        workRepositoryImpl: WorkRepositoryImpl
    ): WorkRepository
    
    @Provides
    @Singleton
    fun provideWorkManager(): WorkManager = PlatformWorkManager()
}
```

## ⚙️ Configuration

### Work Constraints

```kotlin
data class Constraints(
    val requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
    val requiresStorageNotLow: Boolean = false,
    val contentUriTriggers: Set<ContentUriTrigger> = emptySet()
) {
    class Builder {
        fun setRequiredNetworkType(networkType: NetworkType): Builder
        fun setRequiresCharging(requiresCharging: Boolean): Builder
        fun setRequiresDeviceIdle(requiresIdle: Boolean): Builder
        fun setRequiresBatteryNotLow(requiresBatteryNotLow: Boolean): Builder
        fun setRequiresStorageNotLow(requiresStorageNotLow: Boolean): Builder
        fun addContentUriTrigger(uri: Uri, triggerForDescendants: Boolean): Builder
        fun build(): Constraints
    }
}

enum class NetworkType {
    NOT_REQUIRED,
    CONNECTED,
    UNMETERED,
    NOT_ROAMING,
    METERED
}
```

### Retry Policies

```kotlin
enum class BackoffPolicy {
    EXPONENTIAL,
    LINEAR
}

data class RetryConfig(
    val maxAttempts: Int = 3,
    val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    val initialDelay: Duration = Duration.ofMinutes(1),
    val maxDelay: Duration = Duration.ofHours(1),
    val multiplier: Double = 2.0
)
```

## 📱 Platform-Specific Implementation

### Android Setup

Add to `AndroidManifest.xml`:

```xml
<application>
    <!-- WorkManager initialization -->
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
        <meta-data
            android:name="io.github.mobilebytelabs.workkit.WorkKitInitializer"
            android:value="androidx.startup" />
    </provider>
</application>
```

### iOS Setup

Configure background tasks in `Info.plist`:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>com.yourapp.background-sync</string>
    <string>com.yourapp.data-processing</string>
</array>
```

### Desktop Configuration

```kotlin
class DesktopWorkManagerConfig {
    val maxConcurrentWorkers: Int = 4
    val workDirectory: String = System.getProperty("user.home") + "/.workkit"
    val enablePersistence: Boolean = true
}
```

## 🧪 Testing

### Unit Testing Workers

```kotlin
class DataSyncWorkerTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private val mockSyncService = mockk<SyncService>()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @Test
    fun `should return success when sync completes successfully`() = runTest {
        // Given
        val worker = DataSyncWorker()
        val inputData = workDataOf(
            DataSyncWorker.KEY_API_KEY to "test_key",
            DataSyncWorker.KEY_SYNC_TYPE to SyncType.INCREMENTAL.name
        )
        val progressCallback = mockk<ProgressCallback>(relaxed = true)
        
        coEvery { mockSyncService.syncData(any(), any()) } returns SyncResult(
            syncedItems = 100,
            success = true
        )
        
        // When
        val result = worker.doWork(inputData, progressCallback)
        
        // Then
        assertTrue(result is WorkResult.Success)
        verify { progressCallback.setProgress(any()) }
    }
    
    @Test
    fun `should return retry when network error occurs`() = runTest {
        // Given
        val worker = DataSyncWorker()
        val inputData = workDataOf(
            DataSyncWorker.KEY_API_KEY to "test_key"
        )
        val progressCallback = mockk<ProgressCallback>(relaxed = true)
        
        coEvery { mockSyncService.syncData(any(), any()) } throws NetworkException("Connection failed")
        
        // When
        val result = worker.doWork(inputData, progressCallback)
        
        // Then
        assertTrue(result is WorkResult.Retry)
        assertEquals("Connection failed", (result as WorkResult.Retry).retryReason)
    }
}
```

### Integration Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class WorkManagerIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var workManager: TestWorkManager
    
    @Before
    fun setup() {
        workManager = TestWorkManager.getInstance(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
    }
    
    @Test
    fun workScheduler_schedulesWorkSuccessfully() {
        var workScheduled = false
        
        composeTestRule.setContent {
            WorkSchedulerScreen(
                workManager = workManager,
                onWorkScheduled = { workScheduled = true }
            )
        }
        
        composeTestRule
            .onNodeWithText("Schedule One-Time Work")
            .performClick()
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            workScheduled
        }
        
        assertTrue(workScheduled)
        assertEquals(1, workManager.enqueuedRequests.size)
    }
}
```

### Mock Work Manager

```kotlin
class TestWorkManager : WorkManager {
    
    val enqueuedRequests = mutableListOf<WorkRequest>()
    private val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
    
    override fun enqueue(request: WorkRequest): Operation {
        enqueuedRequests.add(request)
        return TestOperation.success()
    }
    
    override fun getWorkInfosByTagLiveData(tag: String): LiveData<List<WorkInfo>> {
        return workInfoLiveData
    }
    
    fun simulateWorkProgress(workId: UUID, progress: WorkProgress) {
        val updatedWorkInfos = workInfoLiveData.value?.map { workInfo ->
            if (workInfo.id == workId) {
                workInfo.copy(progress = progress)
            } else {
                workInfo
            }
        } ?: emptyList()
        
        workInfoLiveData.value = updatedWorkInfos
    }
}
```

## 🚀 Advanced Usage

### Work Chaining

```kotlin
class WorkChainBuilder {
    
    fun buildDataProcessingChain(): WorkContinuation {
        val downloadWork = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        val processWork = OneTimeWorkRequestBuilder<ProcessDataWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .build()
            )
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .build()
        
        return WorkManager.getInstance()
            .beginWith(downloadWork)
            .then(processWork)
            .then(uploadWork)
    }
}
```

### Custom Progress Tracking

```kotlin
class DetailedProgressWorker : CoroutineWorker() {
    
    override suspend fun doWork(
        inputData: WorkData,
        progressCallback: ProgressCallback
    ): WorkResult {
        val totalSteps = 5
        val stepProgress = 100 / totalSteps
        
        // Step 1: Initialize
        progressCallback.setProgress(
            WorkProgress(
                progress = stepProgress,
                statusMessage = "Initializing...",
                metadata = mapOf(
                    "current_step" to "initialization",
                    "estimated_time_remaining" to "4 minutes"
                )
            )
        )
        
        // Perform initialization
        delay(1000)
        
        // Step 2: Download data
        progressCallback.setProgress(
            WorkProgress(
                progress = stepProgress * 2,
                statusMessage = "Downloading data...",
                metadata = mapOf(
                    "current_step" to "download",
                    "bytes_downloaded" to "1024000",
                    "total_bytes" to "5120000"
                )
            )
        )
        
        // Continue with remaining steps...
        
        return WorkResult.success()
    }
}
```

### Conditional Work Execution

```kotlin
class ConditionalWorker : CoroutineWorker() {
    
    override suspend fun doWork(
        inputData: WorkData,
        progressCallback: ProgressCallback
    ): WorkResult {
        val userPreferences = getUserPreferences()
        val networkState = getNetworkState()
        val batteryLevel = getBatteryLevel()
        
        // Check custom conditions
        if (!userPreferences.allowBackgroundSync) {
            return WorkResult.failure("Background sync disabled by user")
        }
        
        if (networkState.isMetered && !userPreferences.allowMeteredSync) {
            return WorkResult.retry("Waiting for unmetered connection")
        }
        
        if (batteryLevel < 20 && !userPreferences.allowLowBatterySync) {
            return WorkResult.retry("Waiting for battery to charge")
        }
        
        // Proceed with work
        return performActualWork(inputData, progressCallback)
    }
}
```

## 📚 Sample Projects

Check out our sample projects in the `/samples` directory:

- **`basic-scheduler`**: Simple background task scheduling
- **`data-sync-app`**: Complete data synchronization example
- **`image-processor`**: Batch image processing with progress tracking
- **`notification-sender`**: Scheduled notification system
- **`file-backup`**: Automated file backup with constraints

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/mobilebytelabs/workkit-kmp.git
   cd workkit-kmp
   ```

2. Set up the development environment:
   ```bash
   ./gradlew build
   ```

3. Run tests:
   ```bash
   ./gradlew allTests
   ```

4. Format code:
   ```bash
   ./gradlew spotlessApply
   ```

### Code Style

This project follows [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and uses:
- [Spotless](https://github.com/diffplug/spotless) for code formatting
- [Detekt](https://detekt.dev/) for static analysis
- [ktlint](https://ktlint.github.io/) for Kotlin linting

## 📄 License

```
Copyright 2024 MobileByteLabs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🙏 Acknowledgments

- [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for Android implementation patterns
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI toolkit
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) for cross-platform development
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) for structured concurrency

## 📞 Support

- 📖 [Documentation](https://mobilebytelabs.github.io/workkit-kmp)
- 🐛 [Issue Tracker](https://github.com/mobilebytelabs/workkit-kmp/issues)
- 💬 [Discussions](https://github.com/mobilebytelabs/workkit-kmp/discussions)
- 📧 [Email Support](mailto:support@mobilebytelabs.com)

---

<div align="center">
Made with ⚡ by MobileByteLabs team
</div>
