package io.github.mobilebytelabs.worker.compose.storeflow

/**
 * Compose helper enum + model for rendering `DraftSubmitHandler` state as a
 * snackbar + button (or any other UI affordance).
 *
 * Added in v3.0.0-alpha03.X (Phase 3 extension).
 *
 * Note: this file uses plain Kotlin types (no `cmp-worker-storeflow` dep) to avoid
 * a hard dependency from `cmp-worker-compose` → `cmp-worker-storeflow`. Consumer
 * code maps its `DraftSubmitHandler.State<P>` to one of the [SubmitStateUi] labels
 * + a [SubmitStateUiModel] for the Composable layer to consume.
 *
 * Example mapping (in consumer code):
 * ```kotlin
 * fun <P : Any> DraftSubmitHandler.State<P>.toUiModel(): SubmitStateUiModel = when (this) {
 *     is DraftSubmitHandler.State.Idle       -> SubmitStateUiModel(SubmitStateUi.IDLE)
 *     is DraftSubmitHandler.State.Drafting   -> SubmitStateUiModel(SubmitStateUi.DRAFTING)
 *     is DraftSubmitHandler.State.Submitting -> SubmitStateUiModel(SubmitStateUi.SUBMITTING, "Submitting…")
 *     is DraftSubmitHandler.State.Submitted  -> SubmitStateUiModel(SubmitStateUi.SUBMITTED, "Done!")
 *     is DraftSubmitHandler.State.Failed     -> SubmitStateUiModel(SubmitStateUi.FAILED, reason)
 * }
 * ```
 *
 * The full Composable scaffold (`SubmitStateScaffold(model, content)`) lands in
 * alpha03.X.Y once the consumer pattern stabilizes. Shipping the enum + model
 * now lets consumers structure their UI state correctly from day one.
 */
public enum class SubmitStateUi {
    /** No draft in progress. */
    IDLE,

    /** Consumer is editing a draft. */
    DRAFTING,

    /** Submit in flight. */
    SUBMITTING,

    /** Submission succeeded. */
    SUBMITTED,

    /** Submission failed; surface retry/cancel affordance. */
    FAILED,
}

/**
 * UI model that consumer code passes to the (alpha03.X.Y) `SubmitStateScaffold`
 * Composable.
 *
 * @property state Current submission state — drives which affordance the scaffold renders.
 * @property message Optional user-facing message (e.g. "Submitting…", failure reason).
 * @property onRetry Optional callback wired to a "Retry" action when [state] = [SubmitStateUi.FAILED].
 * @property onCancel Optional callback wired to a "Cancel" action when [state] ∈ {SUBMITTING, FAILED}.
 */
public data class SubmitStateUiModel(
    public val state: SubmitStateUi,
    public val message: String? = null,
    public val onRetry: (() -> Unit)? = null,
    public val onCancel: (() -> Unit)? = null,
)
