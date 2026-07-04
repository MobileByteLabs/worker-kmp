package io.github.mobilebytelabs.worker.app.gradle

import io.github.mobilebytelabs.worker.app.gradle.codegen.AutoShimGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.WorkerInitGenerator
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plugin-side coverage for the "Shape 2" bring-your-own-Application path
 * (GitHub issue #51, bug 2). A workers-only [CodegenModel] (`appGeneration = false`,
 * empty identity fields) must still produce the worker registry + `WorkerKmpAuto`
 * install shim — the launcher codegen is what gets skipped, not worker registration.
 */
class WorkersOnlyCodegenTest {

    private fun workersOnlyModel() = CodegenModel(
        title = "",
        iosBundleId = "",
        webCanvasId = "composeCanvas",
        androidApplicationId = "",
        androidPermissions = emptyList(),
        packageName = "com.example.app",
        koinModulesFnFqn = "",
        koinModulesFnTakesFactory = false,
        contentFnFqn = "",
        workers = listOf(
            WorkerDef(fqn = "com.example.work.DataSyncWorker", koinDeps = emptyList()),
        ),
        appGeneration = false,
    )

    private fun tempDir(prefix: String): File = Files.createTempDirectory(prefix).toFile()

    @Test
    fun `worker registry is generated from a workers-only model`() {
        val out = tempDir("workers-only-init")
        try {
            WorkerInitGenerator.run(workersOnlyModel(), WorkerInitGenerator.Platform.Android, out)
            val file = File(out, "androidMain/kotlin/com/example/app/generated/Generated_WorkerKmpInit.kt")
            assertTrue(file.exists(), "expected generated registry at $file")
            val text = file.readText()
            assertTrue("com.example.work.DataSyncWorker" in text, "registry must reference the declared worker")
        } finally {
            out.deleteRecursively()
        }
    }

    @Test
    fun `WorkerKmpAuto install shim is generated (common expect + platform actual)`() {
        val out = tempDir("workers-only-shim")
        try {
            AutoShimGenerator.runCommon(workersOnlyModel(), out)
            val common = File(out, "commonMain/kotlin/com/example/app/generated/WorkerKmpAuto.kt")
            assertTrue(common.exists(), "expected commonMain WorkerKmpAuto at $common")

            AutoShimGenerator.runPlatformActual(workersOnlyModel(), AutoShimGenerator.Platform.Android, out)
            val actual = File(out, "androidMain/kotlin/com/example/app/generated/WorkerKmpAuto.kt")
            assertTrue(actual.exists(), "expected androidMain WorkerKmpAuto actual at $actual")
        } finally {
            out.deleteRecursively()
        }
    }

    @Test
    fun `appGeneration defaults to true for Shape 1 and is false for workers-only`() {
        val shape1 = CodegenModel(
            title = "MyApp",
            iosBundleId = "com.example.ios",
            webCanvasId = "composeCanvas",
            androidApplicationId = "com.example",
            androidPermissions = emptyList(),
            packageName = "com.example",
            koinModulesFnFqn = "com.example.appModules",
            contentFnFqn = "com.example.AppContent",
        )
        assertTrue(shape1.appGeneration, "Shape 1 model must default to app generation")
        assertFalse(workersOnlyModel().appGeneration, "workers-only model must disable app generation")
    }
}
