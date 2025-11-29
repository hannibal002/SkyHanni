package at.hannibal2.skyhanni.config

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class VersionConstraintProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return VersionConstraintProcessor(
            environment.codeGenerator,
            environment.logger,
        )
    }
}
