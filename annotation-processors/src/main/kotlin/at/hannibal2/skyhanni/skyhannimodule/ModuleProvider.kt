package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ModuleProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ModuleProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["skyhanni.modver"] ?: "0.0.0",
            environment.options["skyhanni.mcver"] ?: "1.8.9",
            environment.options["skyhanni.buildpaths"],
        )
    }
}
