package at.hannibal2.hanni.hannimodule

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ModuleProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ModuleProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["hanni.modver"] ?: "0.0.0",
            environment.options["hanni.mcver"] ?: "1.8.9",
            environment.options["hanni.buildpaths"],
        )
    }
}
