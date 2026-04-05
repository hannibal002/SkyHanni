package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class PrimaryFunctionProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = PrimaryFunctionProcessor(
        environment.codeGenerator,
        environment.logger,
        environment.options["skyhanni.modver"] ?: "0.0.0",
    )
}
