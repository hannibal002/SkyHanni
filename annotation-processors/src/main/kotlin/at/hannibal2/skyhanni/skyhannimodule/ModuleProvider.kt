package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ModuleProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = ModuleProcessor(
        environment.codeGenerator,
        environment.logger,
        environment.options["skyhanni.modver"] ?: "0.0.0",
        environment.options["skyhanni.mcver"] ?: "1.21.11",
        environment.options["skyhanni.buildpaths"],
        environment.options["skyhanni.cachedir"],
    )
}
