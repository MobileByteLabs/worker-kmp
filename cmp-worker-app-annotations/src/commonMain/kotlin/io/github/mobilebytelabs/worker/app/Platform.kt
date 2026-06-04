/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker.app

/**
 * The four target platforms supported by worker-kmp.
 *
 * Used by [WorkerForPlatforms] to filter which platforms a particular worker class is
 * registered on. See [WorkerForPlatforms] KDoc for usage.
 */
public enum class Platform {
    Android,
    Ios,
    Desktop,
    Web,
}
