package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal actual fun onlineWatcher(): Flow<Unit> = emptyFlow()
