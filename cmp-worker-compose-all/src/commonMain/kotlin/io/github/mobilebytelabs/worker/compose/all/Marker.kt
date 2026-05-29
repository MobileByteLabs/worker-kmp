/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package io.github.mobilebytelabs.worker.compose.all

/**
 * Marker artifact for the all-in-one bundle.
 *
 * cmp-worker-compose-all is an `api(project(...))` re-export module — it has no
 * functional code of its own. But Maven's per-target publications expect a
 * non-empty klib (`build/libs/cmp-worker-compose-all-iosArm64Main-X.Y.Z.klib`
 * etc.) on disk during `generateMetadataFileFor{Target}Publication`. Without
 * any source, no klib is produced and the publish task fails with
 * `java.io.FileNotFoundException`.
 *
 * This marker file gives the compiler enough commonMain source to produce
 * empty-but-real klibs for every target the module declares. Bundled in the
 * published artifact at no semantic cost — just a single internal val.
 */
internal val bundleMarker: String = "io.github.mobilebytelabs:worker-compose-all"
