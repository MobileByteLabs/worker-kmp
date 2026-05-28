package io.github.mobilebytelabs.worker.config

/**
 * Configuration for installing the desktop daemon via the host OS scheduler.
 *
 * Lives in commonMain (since v3.0.0-alpha05.X, Phase 8 alpha05.X) so consumers declare the
 * background-scheduling knobs from a single shared-config entry point. The full installer
 * implementations live in `cmp-worker-desktop-daemon`; this type is referenced from
 * [DesktopWorkerConfig.background] and consumed reflectively from `cmp-worker-desktop` to
 * avoid a hard module dependency.
 *
 * @param appId Reverse-DNS-ish identifier used to name the per-OS task/agent/timer/cron entry
 *   (e.g. `com.example.app` → Windows task `WorkerKmp-com.example.app`, macOS plist label
 *   `com.example.app.worker-kmp`, systemd unit `com.example.app.worker-kmp.timer`).
 * @param daemonJarPath Absolute path to the daemon JAR. Consumer apps typically ship the
 *   daemon JAR alongside the installed app binary and resolve the path at runtime.
 * @param runtimeJavaHome Absolute path to the `java` (or `javaw.exe` on Windows) binary the
 *   scheduler should invoke. `null` falls back to platform defaults (`javaw.exe` on Windows
 *   from PATH, `/usr/bin/java` elsewhere).
 * @param persistenceDir Filesystem directory the daemon scans for pending `.properties` work
 *   entries. `null` (default) resolves to `~/.worker-kmp` on the JVM side at construction
 *   time — done platform-side because commonMain can't reference `System.getProperty(...)`.
 * @param pollIntervalMin Minutes between scheduler firings.
 * @param installOnFirstRun When true, `desktopWorkManagerFactory(...)` invokes the installer
 *   reflectively at first construction if the daemon isn't already installed.
 * @param uninstallOnAppUninstall Best-effort hint for consumer uninstallers (no automatic
 *   enforcement in the library — consumer drives this from their own uninstall hook).
 * @param runOnlyIfLoggedOn Windows-only — when true, the task fires only while a user is
 *   logged on (the default; matches user-mode workers). When false, the task can fire
 *   without an active session (requires `runOnlyIfLoggedOn=false` consent at install time).
 */
public data class DesktopBackgroundConfig(
    public val appId: String,
    public val daemonJarPath: String,
    public val runtimeJavaHome: String? = null,
    public val persistenceDir: String? = null,
    public val pollIntervalMin: Int = 15,
    public val installOnFirstRun: Boolean = true,
    public val uninstallOnAppUninstall: Boolean = true,
    public val runOnlyIfLoggedOn: Boolean = false,
)
