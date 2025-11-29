package at.hannibal2.skyhanni.config

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class VersionConstraintProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val onlyLegacy = resolver.getSymbolsWithAnnotation(OnlyLegacy::class.qualifiedName!!).toSet()
        val onlyModern = resolver.getSymbolsWithAnnotation(OnlyModern::class.qualifiedName!!).toSet()

        (onlyLegacy intersect onlyModern).forEach { symbol ->
            logger.error("@OnlyLegacy and @OnlyModern cannot be used together on the same option", symbol)
        }

        return emptyList()
    }
}
