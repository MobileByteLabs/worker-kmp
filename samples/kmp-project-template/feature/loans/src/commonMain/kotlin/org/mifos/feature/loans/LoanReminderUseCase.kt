package org.mifos.feature.loans

import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.oneTimeWorkRequest
import io.github.mobilebytelabs.worker.scheduler.WorkMode
import io.github.mobilebytelabs.worker.scheduler.WorkScheduler
import io.github.mobilebytelabs.worker.scheduler.enqueueDataSync
import io.github.mobilebytelabs.worker.scheduler.scheduleDailyDataSync
import io.github.mobilebytelabs.worker.scheduler.scheduleDataSyncAtExact
import io.github.mobilebytelabs.worker.workDataOf
import org.mifos.sync.DataSyncWorker
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.mifos.sync.NotificationContent
import org.mifos.sync.NotificationWorker

/**
 * Cross-module usage demo (D18 + D24).
 *
 * Constructor-injects [WorkScheduler] (for sync scheduling) and raw [WorkManager]
 * (for sample-owned notification scheduling). The split reflects the cmp-worker-scheduler
 * library's posture: schedule sync via WorkScheduler; schedule any other worker
 * (notifications, custom domain workers) via raw WorkManager.
 *
 * Calls:
 *   1. `scheduler.scheduleDailyDataSync(LocalTime(9, 0))` — refresh exchange rates +
 *      macro indicators every morning at 9 AM (library responsibility).
 *   2. `scheduler.scheduleDataSyncAtExact(paymentDueInstant)` — exact-alarm sync
 *      right before the payment-due timestamp (library responsibility).
 *   3. `workManager.enqueue(oneTimeWorkRequest<NotificationWorker> { ... })` —
 *      fire the reminder notification at the user's preferred hour (sample's own
 *      NotificationWorker class + sample's own renderNotification expect/actual).
 */
@OptIn(ExperimentalTime::class)
class LoanReminderUseCase(
    private val workScheduler: WorkScheduler,
    private val workManager: WorkManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    /** Set up the daily morning refresh — called once at app start. */
    fun installDailyRefresh(borrowerCountryCode: String = "US") {
        workScheduler.scheduleDailyDataSync<DataSyncWorker>(
            timeOfDay = LocalTime(hour = 9, minute = 0),
            timeZone = TimeZone.currentSystemDefault(),
            payload = workDataOf(
                "currency.base" to "USD",
                "macro.countries" to borrowerCountryCode,
            ),
        )
    }

    /** Schedule a loan-payment reminder for a specific loan + due instant. */
    fun scheduleReminder(loanId: String, dueAt: Instant, currencyBase: String = "USD") {
        // 1. Fire the user-visible reminder at dueAt — raw WorkManager enqueue
        //    of the sample's own NotificationWorker (consumer-owned; not in library).
        val notifContent = NotificationContent(
            title = "Loan payment due",
            body = "Payment for loan #$loanId is due today.",
            channelId = "loan-reminders",
        )
        val delayMs = (dueAt - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0)
        val notifRequest = oneTimeWorkRequest<NotificationWorker> {
            setInputData(
                workDataOf(
                    "title" to notifContent.title,
                    "body" to notifContent.body,
                    "channelId" to (notifContent.channelId ?: ""),
                ),
            )
            setInitialDelay(delayMs.milliseconds)
            addTag("loan-reminder-$loanId")
        }
        scope.launch { workManager.enqueue(notifRequest) }

        // 2. Sync exchange rates RIGHT BEFORE the due instant so the
        //    loan-details screen renders with fresh data when the user opens it.
        val refreshAt = dueAt.minus(15.minutes)
        workScheduler.scheduleDataSyncAtExact<DataSyncWorker>(
            instant = refreshAt,
            mode = WorkMode.Background,
            payload = workDataOf(
                "currency.base" to currencyBase,
                "loan.id" to loanId,
            ),
        )
    }

    /** Manual refresh button — pull-to-refresh from the loans list. */
    fun refreshNow() {
        workScheduler.enqueueDataSync<DataSyncWorker>(
            mode = WorkMode.Background,
            payload = workDataOf("currency.base" to "USD"),
        )
    }
}
