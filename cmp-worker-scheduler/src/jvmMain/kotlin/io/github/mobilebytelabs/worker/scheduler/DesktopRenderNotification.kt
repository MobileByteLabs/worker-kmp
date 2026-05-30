package io.github.mobilebytelabs.worker.scheduler

import java.awt.AWTException
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.TrayIcon.MessageType

/**
 * Desktop actual for [renderNotification].
 *
 * Uses [SystemTray] when a display is available. Headless environments (CI, Docker,
 * remote servers) and unsupported OSes fall back to a stderr log line so the call never throws.
 */
actual fun renderNotification(content: NotificationContent) {
    if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
        System.err.println(
            "[DesktopRenderNotification] headless/no-tray — suppressed: " +
                "${content.title}: ${content.body}",
        )
        return
    }

    val iconUrl = ClassLoader.getSystemResource("notification_icon.png")
    if (iconUrl == null) {
        System.err.println(
            "[DesktopRenderNotification] notification_icon.png not found on classpath — " +
                "skipping tray notification: ${content.title}",
        )
        return
    }
    val image: Image = java.awt.Toolkit.getDefaultToolkit().createImage(iconUrl)

    val trayIcon = TrayIcon(image, content.title)
    trayIcon.isImageAutoSize = true

    try {
        SystemTray.getSystemTray().add(trayIcon)
        trayIcon.displayMessage(content.title, content.body, MessageType.INFO)
    } catch (e: AWTException) {
        System.err.println("[DesktopRenderNotification] tray-add failed: ${e.message}")
    }
}
