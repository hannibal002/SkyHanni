package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class VersionConstantsProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = VersionConstantsProcessor(
        environment.codeGenerator,
        environment.logger,
        environment.options["skyhanni.modver"] ?: "0.0.0",
        environment.options["skyhanni.mcver"] ?: "1.21.10",
    )
}
