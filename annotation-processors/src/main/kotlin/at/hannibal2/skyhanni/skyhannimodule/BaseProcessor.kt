package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

abstract class BaseProcessor(
    protected val codeGenerator: CodeGenerator,
    protected val logger: KSPLogger,
    protected val modVersion: String,
) : SymbolProcessor {

    private var processed = false

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed || modVersion == "0.0.0") return emptyList()
        processed = true
        return processSymbols(resolver)
    }

    protected abstract fun processSymbols(resolver: Resolver): List<KSAnnotated>
}
