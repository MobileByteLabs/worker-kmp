package io.github.mobilebytelabs.worker.app.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * KSP SymbolProcessor for the worker-kmp-app-plugin.
 *
 * Discovers `@WorkerKmpApp` + `@WorkerKmpAppContent` in the consumer's commonMain,
 * validates contract requirements (exactly one of each, top-level, correct shape),
 * and emits a serialized [CodegenModel] to:
 *
 *   build/generated/ksp/metadata/commonMain/resources/META-INF/worker-kmp-app/codegen-model.json
 *
 * The Gradle plugin reads that file at codegen-task time and uses it to template
 * the per-platform launcher files. Failures here produce KSP compile errors with
 * the message body — KSP surfaces them as `e: file:line` build failures.
 */
public class WorkerKmpAppProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) :
    SymbolProcessor {

    private companion object {
        const val WORKER_KMP_APP_FQN = "io.github.mobilebytelabs.worker.app.WorkerKmpApp"
        const val WORKER_KMP_APP_CONTENT_FQN = "io.github.mobilebytelabs.worker.app.WorkerKmpAppContent"
        const val MODEL_PACKAGE = ""
        const val MODEL_FILE_NAME = "codegen-model"
        const val MODEL_FILE_EXT = "json"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFns = resolver
            .getSymbolsWithAnnotation(WORKER_KMP_APP_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()
        val contentFns = resolver
            .getSymbolsWithAnnotation(WORKER_KMP_APP_CONTENT_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        // Nothing annotated yet (first round of multi-round processing, or consumer
        // hasn't applied annotations yet). No-op; nothing to defer.
        if (appFns.isEmpty() && contentFns.isEmpty()) return emptyList()

        if (appFns.size != 1) {
            logger.error(
                "worker-kmp-app: expected exactly one @WorkerKmpApp-annotated function; " +
                    "found ${appFns.size}. Each consumer commonMain must declare a single one.",
            )
            return emptyList()
        }
        if (contentFns.size != 1) {
            logger.error(
                "worker-kmp-app: expected exactly one @WorkerKmpAppContent-annotated function; " +
                    "found ${contentFns.size}. Each consumer commonMain must declare a single one.",
            )
            return emptyList()
        }

        val appFn = appFns.first()
        val contentFn = contentFns.first()

        val ann = appFn.annotations.firstOrNull {
            it.shortName.asString() == "WorkerKmpApp"
        } ?: run {
            logger.error(
                "worker-kmp-app: could not resolve @WorkerKmpApp annotation on ${appFn.qualifiedName?.asString()}",
            )
            return emptyList()
        }

        val model = CodegenModel(
            title = argString(ann, "title") ?: return errorMissing("title", appFn),
            iosBundleId = argString(ann, "iosBundleId") ?: return errorMissing("iosBundleId", appFn),
            webCanvasId = argString(ann, "webCanvasId") ?: "composeCanvas",
            androidApplicationId = argString(ann, "androidApplicationId") ?: "",
            androidPermissions = argStringList(ann, "androidPermissions"),
            packageName = appFn.packageName.asString(),
            koinModulesFnFqn = appFn.qualifiedName?.asString() ?: return errorUnresolvable("@WorkerKmpApp function"),
            contentFnFqn =
            contentFn.qualifiedName?.asString() ?: return errorUnresolvable("@WorkerKmpAppContent function"),
        )

        // Write the model — single aggregating output so re-runs replace it cleanly.
        codeGenerator.createNewFile(
            dependencies = Dependencies(
                aggregating = true,
                appFn.containingFile!!,
                contentFn.containingFile!!,
            ),
            packageName = MODEL_PACKAGE,
            fileName = MODEL_FILE_NAME,
            extensionName = MODEL_FILE_EXT,
        ).use { stream ->
            stream.write(Json.encodeToString(model).toByteArray(Charsets.UTF_8))
        }
        logger.info("worker-kmp-app: emitted codegen-model.json for ${model.koinModulesFnFqn}")
        return emptyList()
    }

    private fun argString(ann: KSAnnotation, name: String): String? =
        ann.arguments.firstOrNull { it.name?.asString() == name }?.value as? String

    @Suppress("UNCHECKED_CAST")
    private fun argStringList(ann: KSAnnotation, name: String): List<String> {
        val raw = ann.arguments.firstOrNull { it.name?.asString() == name }?.value ?: return emptyList()
        return when (raw) {
            is List<*> -> raw.map { it.toString() }
            is Array<*> -> raw.map { it.toString() }
            else -> emptyList()
        }
    }

    private fun errorMissing(fieldName: String, fn: KSFunctionDeclaration): List<KSAnnotated> {
        logger.error(
            "worker-kmp-app: required @WorkerKmpApp argument '$fieldName' missing on ${fn.qualifiedName?.asString()}",
        )
        return emptyList()
    }

    private fun errorUnresolvable(what: String): List<KSAnnotated> {
        logger.error("worker-kmp-app: could not resolve fully-qualified name of $what")
        return emptyList()
    }
}
