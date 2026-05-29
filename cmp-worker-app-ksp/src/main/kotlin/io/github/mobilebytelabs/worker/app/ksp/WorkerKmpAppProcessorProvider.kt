package io.github.mobilebytelabs.worker.app.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP service-loader entry. Registered via
 * `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.
 */
public class WorkerKmpAppProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = WorkerKmpAppProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
    )
}
