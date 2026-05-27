package io.github.mobilebytelabs.worker

/**
 * Marker opt-in annotation for the foreground task APIs.
 *
 * Added in v3.0.0-alpha01 (Phase 1 of the v3.0.0 epic). Marked experimental
 * until v3.0.0 GA per Phase 1 T27 — APIs stable, per-platform actual impls
 * land per-platform in alpha01.X follow-ups.
 */
@RequiresOptIn(
    message = "Foreground task APIs are experimental until v3.0.0 GA. " +
        "Opt-in: @OptIn(ExperimentalForegroundApi::class) at use site.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class ExperimentalForegroundApi

/**
 * Long-running, user-visible work that runs outside the platform's background
 * execution window. Extends [CoroutineWorker] with a [setForeground] handshake
 * the worker calls to promote itself to foreground.
 *
 * Per-platform actual behavior:
 * - **Android**: calls `androidx.work.CoroutineWorker.setForeground(ForegroundInfo)`
 *   with the notification supplied via [ForegroundInfo]. Requires
 *   FOREGROUND_SERVICE permission + (on Android 14+) the
 *   `android:foregroundServiceType` declaration matching [ForegroundInfo.serviceType].
 * - **iOS 17+**: schedules a `BGContinuedProcessingTaskRequest` with progress
 *   reporting via `BGContinuedProcessingTaskUpdate`.
 * - **iOS 13-16**: degrades to `BGProcessingTaskRequest` + posts a
 *   `UNNotification` if [ForegroundInfo.title] is non-null.
 * - **Desktop**: registers a `java.awt.SystemTray` icon with progress + cancel
 *   menu item. Survives JFrame.dispose() via the supervisor scope keepAlive
 *   pattern.
 * - **Web**: registers a long-lived Service Worker that owns foreground
 *   coroutines. Survives tab close (subject to browser eviction policy).
 *
 * Consumer use:
 * ```kotlin
 * @OptIn(ExperimentalForegroundApi::class)
 * class FileUploadWorker(context: WorkerContext) : ForegroundWorker(context) {
 *     override suspend fun doWork(): WorkResult {
 *         setForeground(
 *             ForegroundInfo(
 *                 notificationId = 42,
 *                 title = "Uploading file",
 *                 message = "0%",
 *                 progress = WorkProgress(0),
 *             ),
 *         )
 *         val file = inputData.getString("filePath") ?: return WorkResult.failure()
 *         uploadFile(file) { pct ->
 *             setForeground(ForegroundInfo.copy(progress = WorkProgress(pct), message = "$pct%"))
 *         }
 *         return WorkResult.success()
 *     }
 * }
 * ```
 *
 * Added in v3.0.0-alpha01.
 *
 * @see ForegroundInfo
 * @see runAsForeground
 */
@ExperimentalForegroundApi
public abstract class ForegroundWorker(context: WorkerContext) : CoroutineWorker(context) {

    /**
     * Promote this worker to user-visible foreground execution. Idempotent — subsequent
     * calls update the existing foreground notification.
     *
     * The worker continues running in the same coroutine; this call is the handshake
     * that asks the platform to keep the work alive past the background window AND
     * surface a notification.
     *
     * @throws ForegroundNotSupportedException if the platform cannot honor the request
     *   (e.g. iOS <17 without UNNotifications permission; Desktop with foregroundTrayEnabled=false).
     */
    public suspend fun setForeground(info: ForegroundInfo) {
        runAsForeground(this, info)
    }
}

/**
 * Foreground notification + progress descriptor.
 *
 * @property notificationId Android notification ID (ignored on iOS/Desktop/Web; supply
 *   any unique-per-worker int).
 * @property title Notification title (and tray label on Desktop).
 * @property message Notification body / status message.
 * @property progress Optional progress; renders as a progress bar on Android + Desktop,
 *   updates the BGContinuedProcessingTaskUpdate on iOS 17+.
 * @property serviceType Required on Android 14+ to match `android:foregroundServiceType`
 *   in the consumer's Manifest. See [ForegroundServiceType] for valid values.
 *   No-op on iOS/Desktop/Web.
 * @property cancelAction Optional cancel callback wired into the notification's
 *   cancel action (Android), tray Cancel menu (Desktop), or notification action (Web/iOS).
 *
 * Added in v3.0.0-alpha01.
 */
@ExperimentalForegroundApi
public data class ForegroundInfo(
    public val notificationId: Int,
    public val title: String,
    public val message: String,
    public val progress: WorkProgress = WorkProgress(0),
    public val serviceType: ForegroundServiceType? = null,
    public val cancelAction: (suspend () -> Unit)? = null,
)

/**
 * Android 14+ foreground service type. Required by the Android Manifest's
 * `android:foregroundServiceType` declaration when [ForegroundInfo.serviceType]
 * is non-null. No-op on iOS/Desktop/Web.
 *
 * Mirror of Android 14's `ServiceInfo.FOREGROUND_SERVICE_TYPE_*` constants.
 *
 * Added in v3.0.0-alpha01.
 */
@ExperimentalForegroundApi
public enum class ForegroundServiceType {
    DATA_SYNC,
    MEDIA_PLAYBACK,
    MEDIA_PROJECTION,
    CONNECTED_DEVICE,
    PHONE_CALL,
    CAMERA,
    MICROPHONE,
    LOCATION,
    HEALTH,
    REMOTE_MESSAGING,
    SHORT_SERVICE,
    SPECIAL_USE,
    SYSTEM_EXEMPTED,
}

/**
 * Per-platform bridge to the OS's foreground promotion mechanism. Internal to the
 * foreground-tasks scaffolding — consumers should NOT call directly; use
 * [ForegroundWorker.setForeground] instead.
 *
 * Per-platform actual implementations land in alpha01.X follow-ups. In alpha01,
 * default `runAsForeground` LOGS the request via kermit (no-op behavior) so the
 * SPI compiles + tests pass against the surface.
 */
@ExperimentalForegroundApi
public expect suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo)

/**
 * Thrown when the platform cannot honor a [ForegroundWorker.setForeground] call.
 *
 * Added in v3.0.0-alpha01.
 */
@ExperimentalForegroundApi
public class ForegroundNotSupportedException(public val platform: String, public val reason: String) :
    RuntimeException("Foreground promotion not supported on $platform: $reason")
