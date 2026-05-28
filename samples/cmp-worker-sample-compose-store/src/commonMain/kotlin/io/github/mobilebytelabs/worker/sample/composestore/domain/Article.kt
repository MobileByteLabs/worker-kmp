package io.github.mobilebytelabs.worker.sample.composestore.domain

data class Article(
    val id: String,
    val title: String,
    val body: String,
    val fetchedAtEpochMs: Long,
)
