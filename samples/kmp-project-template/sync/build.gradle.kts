// File: samples/kmp-project-template/sync/build.gradle.kts
plugins {
    alias(libs.plugins.cmp.feature.convention)
    alias(libs.plugins.worker.compose.convention)
}

android { namespace = "org.mifos.sync" }

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:data"))
        // cmp-worker-compose-all (via convention plugin) brings cmp-worker-scheduler
        // transitively — WorkScheduler, DefaultWorkScheduler, SyncStatePersister,
        // AbstractDataSyncWorker, Synchronizer/Syncable, NotificationWorker, etc.
        // kotlinx-coroutines and kotlinx-serialization come transitively via the library.
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core.ktx)  // NotificationManagerCompat
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
    }
}
