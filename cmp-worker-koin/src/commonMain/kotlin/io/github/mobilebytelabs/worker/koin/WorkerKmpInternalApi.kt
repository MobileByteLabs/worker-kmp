/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker.koin

/**
 * Marker for worker-kmp APIs intended ONLY for code emitted by `cmp-worker-app-plugin`'s
 * codegen. Direct consumer use is unsupported and may break without notice.
 *
 * Closes audit gap H1 — `internal` visibility would have blocked the codegen-emitted
 * `Generated_WorkerKmpInit.kt` in CONSUMER modules from calling the function across module
 * boundaries (Kotlin module-boundary rules). The opt-in marker pattern keeps the function
 * callable across modules (it's still `public` in Kotlin's visibility sense) while
 * `@RequiresOptIn(level = ERROR)` forces any consumer code attempting to call it to add
 * `@OptIn(WorkerKmpInternalApi::class)` — surfacing the "you're calling something not meant
 * for you" signal at compile time.
 *
 * Codegen-emitted files annotate their file with `@file:OptIn(WorkerKmpInternalApi::class)`
 * — consumer never sees the marker. Direct consumer call sites fail to compile with the
 * opt-in error level + a clear migration message in [RequiresOptIn.message].
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This worker-kmp API is intended only for code emitted by cmp-worker-app-plugin's codegen. " +
        "Direct consumer use is unsupported and may break without notice. " +
        "See https://github.com/MobileByteLabs/worker-kmp/wiki/single-api-guide for the consumer-facing API.",
)
@Retention(AnnotationRetention.BINARY)
public annotation class WorkerKmpInternalApi
