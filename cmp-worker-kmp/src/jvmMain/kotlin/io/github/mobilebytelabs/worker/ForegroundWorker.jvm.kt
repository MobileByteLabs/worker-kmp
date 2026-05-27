@file:OptIn(ExperimentalForegroundApi::class)

package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger
import java.awt.AWTException
import java.awt.Color
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop (JVM) actual for [runAsForeground].
 *
 * Surfaces a long-running task to the user via [java.awt.SystemTray]:
 * - Adds (and reuses) a tray icon keyed by [ForegroundInfo.notificationId]
 * - Updates the tooltip with `title — message (progress%)` on every call
 * - Removes the tray icon when `progress == 100` so completed work disappears
 *
 * **Headless / unsupported environments**: on JVMs without an AWT desktop
 * (headless servers, CI runners, JLink images without `java.desktop`)
 * [SystemTray.isSupported] returns `false`. The call degrades to a kermit WARN log
 * — the worker keeps running, just without a tray surface. This matches the
 * documented Desktop contract on [ForegroundWorker] (notification UI is best-effort).
 *
 * Replaces the log-only stub from alpha01.
 *
 * Added in v3.0.0-alpha01.X (Phase 1 deep impl).
 *
 * Android consumers do NOT route through this actual — they go through
 * `cmp-worker-android`'s `KmpAndroidWorker` which calls `setForegroundAsync` on
 * `androidx.work.CoroutineWorker` directly. The Android Foreground bridge lives
 * in `cmp-worker-android` because `cmp-worker-kmp` is JVM-only (no `android()`
 * target) — see Phase 1 alpha01.X deviation note in CHANGELOG.
 */
@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    // Android consumers ship the JVM jar of cmp-worker-kmp at runtime; route them to the
    // androidx.work-aware bridge in cmp-worker-android instead of SystemTray (which crashes
    // on Android). Discovery is reflective so cmp-worker-kmp keeps zero Android dependencies.
    if (isAndroidRuntime() && dispatchToAndroidBridge(worker, info)) return
    if (!isSystemTraySupported()) {
        Logger.withTag("worker-kmp.foreground.desktop").w {
            "SystemTray not supported (headless JVM or missing java.desktop). " +
                "Foreground promotion is log-only. title=${info.title} progress=${info.progress.progress}"
        }
        return
    }
    runCatching {
        val tray = SystemTray.getSystemTray()
        val icon = ensureTrayIcon(tray, info)
        icon.toolTip = formatTooltip(info)
        if (info.progress.progress >= COMPLETE_PROGRESS) {
            tray.remove(icon)
            trayIcons.remove(info.notificationId)
        }
    }.onFailure { t ->
        Logger.withTag("worker-kmp.foreground.desktop").w(t) {
            "Failed to update SystemTray icon for notificationId=${info.notificationId}"
        }
    }
    Logger.withTag("worker-kmp.foreground.desktop").d {
        "Desktop tray updated: id=${info.notificationId} title=${info.title} progress=${info.progress.progress}"
    }
}

private const val COMPLETE_PROGRESS = 100
private const val TRAY_ICON_DIMENSION = 16

// Survives across multiple setForeground() calls so the same notificationId reuses
// the same tray icon (matches Android's notificationId reuse semantics).
private val trayIcons = ConcurrentHashMap<Int, TrayIcon>()

internal fun isSystemTraySupported(): Boolean = runCatching { SystemTray.isSupported() }.getOrDefault(false)

private fun ensureTrayIcon(tray: SystemTray, info: ForegroundInfo): TrayIcon =
    trayIcons.computeIfAbsent(info.notificationId) {
        val image = BufferedImage(TRAY_ICON_DIMENSION, TRAY_ICON_DIMENSION, BufferedImage.TYPE_INT_ARGB).apply {
            val g = createGraphics()
            try {
                g.color = Color(33, 150, 243)
                g.fillRect(0, 0, TRAY_ICON_DIMENSION, TRAY_ICON_DIMENSION)
            } finally {
                g.dispose()
            }
        }
        val icon = TrayIcon(image, info.title).apply { isImageAutoSize = true }
        try {
            tray.add(icon)
        } catch (e: AWTException) {
            Logger.withTag("worker-kmp.foreground.desktop").w(e) {
                "SystemTray.add() refused — tray icon will be log-only for id=${info.notificationId}"
            }
        }
        icon
    }

private fun formatTooltip(info: ForegroundInfo): String =
    "${info.title} — ${info.message} (${info.progress.progress}%)"

// ── Android bridge dispatch (reflective — keeps Android dependencies out of cmp-worker-kmp) ──

private const val ANDROID_BRIDGE_FQCN = "io.github.mobilebytelabs.worker.android.AndroidForegroundBridge"
private const val ANDROID_BRIDGE_INSTANCE_FIELD = "INSTANCE"
private const val ANDROID_BRIDGE_METHOD = "promote"

private fun isAndroidRuntime(): Boolean {
    val runtimeName = runCatching { System.getProperty("java.runtime.name").orEmpty() }.getOrDefault("")
    val vmName = runCatching { System.getProperty("java.vm.name").orEmpty() }.getOrDefault("")
    return runtimeName.contains("Android Runtime", ignoreCase = true) ||
        vmName.contains("Dalvik", ignoreCase = true) ||
        vmName.contains("ART", ignoreCase = true)
}

private fun dispatchToAndroidBridge(worker: ForegroundWorker, info: ForegroundInfo): Boolean {
    return runCatching {
        val cls = Class.forName(ANDROID_BRIDGE_FQCN)
        val instance = cls.getField(ANDROID_BRIDGE_INSTANCE_FIELD).get(null)
        val method = cls.getMethod(ANDROID_BRIDGE_METHOD, ForegroundWorker::class.java, ForegroundInfo::class.java)
        method.invoke(instance, worker, info)
        true
    }.getOrElse { t ->
        Logger.withTag("worker-kmp.foreground").w(t) {
            "Android runtime detected but $ANDROID_BRIDGE_FQCN unavailable — " +
                "include cmp-worker-android in your dependencies for foreground tasks."
        }
        false
    }
}
