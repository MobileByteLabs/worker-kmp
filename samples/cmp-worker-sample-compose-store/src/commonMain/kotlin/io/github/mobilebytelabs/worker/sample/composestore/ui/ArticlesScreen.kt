package io.github.mobilebytelabs.worker.sample.composestore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.compose.WorkInfoCard
import io.github.mobilebytelabs.worker.compose.rememberWorkManager
import io.github.mobilebytelabs.worker.oneTimeWorkRequest
import io.github.mobilebytelabs.worker.sample.composestore.domain.Article
import io.github.mobilebytelabs.worker.sample.composestore.workers.ArticleSyncWorker
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

private const val SYNC_TAG = "article-sync"

/**
 * Interactive screen demonstrating the full kmp + store + koin + compose pipeline.
 *
 *  - User picks an article id and taps Sync.
 *  - `WorkManager.enqueue` schedules an [ArticleSyncWorker] tagged [SYNC_TAG].
 *  - The worker forces a Store5 fresh fetch; the result is cached + emitted back.
 *  - This screen observes both the WorkInfo flow (live progress cards) AND the Store
 *    cached stream (article details panel). They reflect the same underlying state
 *    via different surfaces — the whole point of the integration.
 */
@Composable
fun ArticlesScreen(store: Store<String, Article>) {
    val workManager = rememberWorkManager()
    val coroutineScope = rememberCoroutineScope()

    var articleId by remember { mutableStateOf("42") }

    val workInfos by workManager
        .getWorkInfosByTag(SYNC_TAG)
        .collectAsState(initial = emptyList())

    val cacheFlow = remember(articleId) {
        store.stream(StoreReadRequest.cached(key = articleId, refresh = false))
    }
    val cacheResponse: StoreReadResponse<Article>? by cacheFlow.collectAsState(initial = null)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = articleId,
                onValueChange = { articleId = it.filter { ch -> ch.isLetterOrDigit() } },
                label = { Text("Article id") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val request = oneTimeWorkRequest<ArticleSyncWorker> {
                        setInputData(workDataOf("articleId" to articleId))
                        addTag(SYNC_TAG)
                    }
                    coroutineScope.launch { workManager.enqueue(request) }
                },
                enabled = articleId.isNotBlank(),
            ) {
                Text("Sync article")
            }
        }

        WorkerCardSection(
            workInfos = workInfos,
            onCancel = { id -> coroutineScope.launch { workManager.cancelWorkById(id) } },
        )

        CachedArticleSection(articleId = articleId, response = cacheResponse)
    }
}

@Composable
private fun WorkerCardSection(workInfos: List<WorkInfo>, onCancel: (Uuid) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Sync workers (${workInfos.size})",
            style = MaterialTheme.typography.titleSmall,
        )
        if (workInfos.isEmpty()) {
            Text(
                text = "No work scheduled yet. Tap Sync article to enqueue an ArticleSyncWorker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            workInfos.takeLast(3).forEach { info ->
                WorkInfoCard(info = info, onCancel = { onCancel(info.id) })
            }
        }
    }
}

@Composable
private fun CachedArticleSection(articleId: String, response: StoreReadResponse<Article>?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Store cache for id=$articleId",
                style = MaterialTheme.typography.titleSmall,
            )
            when (response) {
                null -> Text(
                    text = "Subscribing to cache stream…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is StoreReadResponse.Data -> {
                    val article = response.value
                    Text(text = article.title, style = MaterialTheme.typography.bodyLarge)
                    Text(text = article.body, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "id=${article.id} • origin=${response.origin}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                is StoreReadResponse.Error -> Text(
                    text = "Store error: ${response.errorMessageOrNull() ?: response::class.simpleName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                is StoreReadResponse.Loading -> Text(
                    text = "Fetching…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is StoreReadResponse.NoNewData -> Text(
                    text = "Nothing cached yet for id=$articleId — tap Sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    text = response::class.simpleName ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun StoreReadResponse.Error.errorMessageOrNull(): String? = when (this) {
    is StoreReadResponse.Error.Exception -> error.message
    is StoreReadResponse.Error.Message -> message
    else -> null
}
