package io.github.mobilebytelabs.worker.app.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * KSP SymbolProcessor for cmp-worker-app-plugin.
 *
 * Discovers `@WorkerKmpApp` + `@WorkerKmpAppContent` + `@WorkerKmpWorkers` in the consumer's
 * commonMain, validates contract requirements, and emits a serialized [CodegenModel] to:
 *
 *   build/generated/ksp/metadata/commonMain/resources/META-INF/worker-kmp-app/codegen-model.json
 *
 * The Gradle plugin reads that file at codegen-task time and uses it to template the
 * per-platform launcher files + the new (v4.0.0) `Generated_WorkerKmpInit.{Platform}.kt`
 * + `WorkerKmpAuto.kt` shim.
 *
 * Validation covered:
 *  - exactly one `@WorkerKmpApp` + one `@WorkerKmpAppContent` (existing)
 *  - per worker class: extends `CoroutineWorker`, has `WorkerContext` as first primary-ctor
 *    param, is `public` (AC-18, AC-19, D18 per H7)
 *  - per worker constructor dep: dep type is `public` (AC-54)
 *  - default-valued primary-ctor params SKIPPED from `koinDeps` (AC-60, D29 per NG12)
 *  - multiple `@WorkerKmpWorkers` sites within same module AGGREGATED (D23 per H2)
 *  - aggregated `WorkerDef` list SORTED by FQN for deterministic output (D28 per NG3/NG7)
 *  - `@WorkerKmpWorkers` is OPTIONAL — absent or empty produces `workers: []` (AC-62, D31)
 *  - `@WorkerForPlatforms` filter read into `WorkerDef.platformsFilter`
 */
public class WorkerKmpAppProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) :
    SymbolProcessor {

    private companion object {
        const val WORKER_KMP_APP_FQN = "io.github.mobilebytelabs.worker.app.WorkerKmpApp"
        const val WORKER_KMP_APP_CONTENT_FQN = "io.github.mobilebytelabs.worker.app.WorkerKmpAppContent"
        const val WORKER_KMP_WORKERS_FQN = "io.github.mobilebytelabs.worker.app.WorkerKmpWorkers"
        const val WORKER_FOR_PLATFORMS_FQN = "io.github.mobilebytelabs.worker.app.WorkerForPlatforms"
        const val WORKER_CONTEXT_FQN = "io.github.mobilebytelabs.worker.WorkerContext"
        const val COROUTINE_WORKER_FQN = "io.github.mobilebytelabs.worker.CoroutineWorker"
        const val WORK_MANAGER_FACTORY_FQN = "io.github.mobilebytelabs.worker.WorkManagerFactory"
        const val MODEL_PACKAGE = ""
        const val MODEL_FILE_NAME = "codegen-model"
        const val MODEL_FILE_EXT = "json"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFns = resolver.getSymbolsWithAnnotation(WORKER_KMP_APP_FQN)
            .filterIsInstance<KSFunctionDeclaration>().toList()
        val contentFns = resolver.getSymbolsWithAnnotation(WORKER_KMP_APP_CONTENT_FQN)
            .filterIsInstance<KSFunctionDeclaration>().toList()
        val workersFns = resolver.getSymbolsWithAnnotation(WORKER_KMP_WORKERS_FQN)
            .filterIsInstance<KSFunctionDeclaration>().toList()

        // Nothing annotated yet (first round of multi-round processing, or consumer hasn't
        // applied annotations yet). No-op; nothing to defer.
        if (appFns.isEmpty() && contentFns.isEmpty() && workersFns.isEmpty()) return emptyList()

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

        val ann = appFn.annotations.firstOrNull { it.shortName.asString() == "WorkerKmpApp" } ?: run {
            logger.error(
                "worker-kmp-app: could not resolve @WorkerKmpApp annotation on ${appFn.qualifiedName?.asString()}",
            )
            return emptyList()
        }

        // Aggregate workers from all @WorkerKmpWorkers annotation sites within this module.
        // D23 (within-module aggregation only per H2 audit finding) + D31 (optional per M1).
        val aggregatedWorkers = scanWorkerSites(workersFns, resolver)
        val sortedWorkers = aggregatedWorkers.sortedBy { it.fqn }
        // Detect duplicates after sorting (AC-46) — adjacent matches.
        for (i in 1 until sortedWorkers.size) {
            if (sortedWorkers[i].fqn == sortedWorkers[i - 1].fqn) {
                logger.error(
                    "worker-kmp-app: duplicate worker registration for ${sortedWorkers[i].fqn} " +
                        "across multiple @WorkerKmpWorkers sites in the same module. " +
                        "Each worker class may appear only once across all annotation sites.",
                )
                return emptyList()
            }
        }

        // Detect whether the @WorkerKmpApp function takes the legacy `(WorkManagerFactory) -> List<Module>`
        // signature, or the v4.0.0 no-arg shorthand. The codegen branches on this so per-platform launchers
        // pass `platformWorkManagerFactory()` only when the consumer's function actually expects it.
        val koinFnParams = appFn.parameters
        val koinFnTakesFactory = when (koinFnParams.size) {
            0 -> false
            1 -> resolveTypeFqn(koinFnParams[0].type) == WORK_MANAGER_FACTORY_FQN
            else -> {
                logger.error(
                    "worker-kmp-app: @WorkerKmpApp function `${appFn.qualifiedName?.asString()}` must " +
                        "be either no-arg `() -> List<Module>` or take a single `WorkManagerFactory` " +
                        "parameter — found ${koinFnParams.size} parameters.",
                )
                return emptyList()
            }
        }

        val model = CodegenModel(
            title = argString(ann, "title") ?: return errorMissing("title", appFn),
            iosBundleId = argString(ann, "iosBundleId") ?: return errorMissing("iosBundleId", appFn),
            webCanvasId = argString(ann, "webCanvasId") ?: "composeCanvas",
            androidApplicationId = argString(ann, "androidApplicationId") ?: "",
            androidPermissions = argStringList(ann, "androidPermissions"),
            packageName = appFn.packageName.asString(),
            koinModulesFnFqn = appFn.qualifiedName?.asString() ?: return errorUnresolvable("@WorkerKmpApp function"),
            koinModulesFnTakesFactory = koinFnTakesFactory,
            contentFnFqn =
            contentFn.qualifiedName?.asString() ?: return errorUnresolvable("@WorkerKmpAppContent function"),
            workers = sortedWorkers,
        )

        // Write the model — single aggregating output so re-runs replace it cleanly.
        val depFiles = buildList<com.google.devtools.ksp.symbol.KSFile> {
            appFn.containingFile?.let { add(it) }
            contentFn.containingFile?.let { add(it) }
            workersFns.forEach { it.containingFile?.let(::add) }
        }
        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *depFiles.toTypedArray()),
            packageName = MODEL_PACKAGE,
            fileName = MODEL_FILE_NAME,
            extensionName = MODEL_FILE_EXT,
        ).use { stream ->
            stream.write(Json.encodeToString(model).toByteArray(Charsets.UTF_8))
        }
        logger.info(
            "worker-kmp-app: emitted codegen-model.json for ${model.koinModulesFnFqn} with ${sortedWorkers.size} workers",
        )
        return emptyList()
    }

    // ── @WorkerKmpWorkers scanning ────────────────────────────────────────────────

    /**
     * Walks every `@WorkerKmpWorkers`-annotated function in the round, extracts the
     * `workers: Array<KClass<*>>` argument, validates each referenced class, and produces
     * the aggregated `WorkerDef` list (unsorted — caller sorts by FQN).
     */
    private fun scanWorkerSites(workersFns: List<KSFunctionDeclaration>, resolver: Resolver): List<WorkerDef> {
        val result = mutableListOf<WorkerDef>()
        for (fn in workersFns) {
            val workersAnn = fn.annotations.firstOrNull {
                it.shortName.asString() == "WorkerKmpWorkers"
            } ?: continue
            val classRefs = (
                workersAnn.arguments.firstOrNull { it.name?.asString() == "workers" }
                    ?.value as? List<*>
                ) ?: emptyList<Any>()
            for (raw in classRefs) {
                val ksType = raw as? KSType ?: continue
                val classDecl = ksType.declaration as? KSClassDeclaration ?: continue
                val def = validateAndDescribeWorker(classDecl) ?: continue
                result += def
            }
        }
        return result
    }

    /**
     * Validates a worker class + extracts a [WorkerDef] from its primary constructor.
     * Returns null (and emits a KSP error) on validation failure.
     */
    private fun validateAndDescribeWorker(klass: KSClassDeclaration): WorkerDef? {
        val classFqn = klass.qualifiedName?.asString() ?: run {
            logger.error("worker-kmp-app: could not resolve fully-qualified name of worker class.")
            return null
        }
        // Visibility check (AC-19 per H7/D18) — worker class must be public.
        if (Modifier.PRIVATE in klass.modifiers || Modifier.PROTECTED in klass.modifiers ||
            Modifier.INTERNAL in klass.modifiers
        ) {
            logger.error(
                "worker-kmp-app: worker class `$classFqn` must be `public` for codegen to " +
                    "reference it from the generated init file. Make the class `public` or " +
                    "move it to the same module as the @WorkerKmpWorkers annotation.",
            )
            return null
        }
        // Superclass check (AC-18) — must extend CoroutineWorker.
        val superFqns = klass.superTypes.mapNotNull {
            (it.resolve().declaration as? KSClassDeclaration)?.qualifiedName?.asString()
        }.toList()
        if (COROUTINE_WORKER_FQN !in superFqns) {
            logger.error(
                "worker-kmp-app: worker class `$classFqn` must extend " +
                    "`io.github.mobilebytelabs.worker.CoroutineWorker`. " +
                    "Found supertypes: ${superFqns.joinToString(", ")}.",
            )
            return null
        }
        // Primary constructor required.
        val ctor = klass.primaryConstructor ?: run {
            logger.error(
                "worker-kmp-app: worker class `$classFqn` must have a primary constructor " +
                    "with `WorkerContext` as the first parameter.",
            )
            return null
        }
        val params = ctor.parameters
        if (params.isEmpty() || resolveTypeFqn(params[0].type) != WORKER_CONTEXT_FQN) {
            logger.error(
                "worker-kmp-app: worker class `$classFqn` primary constructor's first " +
                    "parameter must be of type `io.github.mobilebytelabs.worker.WorkerContext`. " +
                    "Found: ${params.firstOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString() ?: "<no params>"}.",
            )
            return null
        }
        // Walk remaining params, building koinDeps (skip default-valued per D29/NG12).
        val koinDeps = mutableListOf<DepType>()
        for (i in 1 until params.size) {
            val param = params[i]
            if (param.hasDefault) continue // AC-60: defaults are passed through; not injected.
            val paramName = param.name?.asString() ?: continue
            val depFqn = resolveTypeFqn(param.type) ?: run {
                logger.error(
                    "worker-kmp-app: could not resolve type of constructor parameter " +
                        "`$paramName` on worker class `$classFqn`.",
                )
                return null
            }
            // Dep-type visibility check (AC-54 per H7/D18).
            val depDecl = param.type.resolve().declaration as? KSClassDeclaration
            if (depDecl != null && (
                    Modifier.PRIVATE in depDecl.modifiers ||
                        Modifier.PROTECTED in depDecl.modifiers ||
                        Modifier.INTERNAL in depDecl.modifiers
                    )
            ) {
                logger.error(
                    "worker-kmp-app: constructor parameter `$paramName: $depFqn` on worker " +
                        "`$classFqn` — dep type must be `public`. Codegen-emitted " +
                        "`getKoin().get<$depFqn>()` lives outside the dep type's module. " +
                        "Change `internal class ${depDecl.simpleName.asString()}` to " +
                        "`public class ${depDecl.simpleName.asString()}`.",
                )
                return null
            }
            // Read optional @Named qualifier on the param.
            val namedQualifier = param.annotations.firstOrNull {
                it.shortName.asString() == "Named"
            }?.let { ann ->
                ann.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String
            }
            koinDeps += DepType(paramName = paramName, fqn = depFqn, namedQualifier = namedQualifier)
        }
        // Optional @WorkerForPlatforms co-annotation on the worker class.
        val platforms: Set<String> = klass.annotations.firstOrNull {
            it.shortName.asString() == "WorkerForPlatforms"
        }?.let { ann ->
            @Suppress("UNCHECKED_CAST")
            val rawPlatforms = ann.arguments.firstOrNull { it.name?.asString() == "platforms" }?.value
            when (rawPlatforms) {
                is List<*> -> rawPlatforms.mapNotNull { (it as? KSType)?.declaration?.simpleName?.asString() }.toSet()
                is Array<*> -> rawPlatforms.mapNotNull { (it as? KSType)?.declaration?.simpleName?.asString() }.toSet()
                else -> emptySet()
            }
        } ?: emptySet()

        return WorkerDef(fqn = classFqn, koinDeps = koinDeps, platformsFilter = platforms)
    }

    private fun resolveTypeFqn(ref: KSTypeReference): String? = ref.resolve().declaration.qualifiedName?.asString()

    // ── Helpers ───────────────────────────────────────────────────────────────────

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
