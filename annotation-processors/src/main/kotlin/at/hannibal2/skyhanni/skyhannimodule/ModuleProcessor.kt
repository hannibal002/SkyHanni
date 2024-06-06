package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

class ModuleProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val symbols = resolver.getSymbolsWithAnnotation(SkyHanniModule::class.qualifiedName!!).toList()
        val validSymbols = symbols.mapNotNull { validateSymbol(it) }

        if (validSymbols.isNotEmpty()) {
            generateFile(validSymbols)
        }

        return emptyList()
    }

    private fun validateSymbol(symbol: KSAnnotated): KSClassDeclaration? {
        if (!symbol.validate()) {
            logger.warn("Symbol is not valid: $symbol")
            return null
        }

        if (symbol !is KSClassDeclaration) {
            logger.error("@SkyHanniModule is only valid on class declarations", symbol)
            return null
        }

        if (symbol.classKind != ClassKind.OBJECT) {
            logger.error("@SkyHanniModule is only valid on kotlin objects", symbol)
            return null
        }

        return symbol
    }

    //TODO remove when KMixins added as it contains KSP annotation helpers.
    private fun isDevAnnotation(klass: KSClassDeclaration): Boolean {
        val annotation = klass.annotations.find { it.shortName.asString() == "SkyHanniModule" } ?: return false
        return annotation.arguments.find { it.name?.asString() == "devOnly" }?.value as? Boolean ?: false
    }

    private fun generateFile(symbols: List<KSClassDeclaration>) {
        val dependencies = symbols.mapNotNull { it.containingFile }.toTypedArray()
        val deps = Dependencies(true, *dependencies)

        val file = codeGenerator.createNewFile(deps, "at.hannibal2.skyhanni.skyhannimodule", "LoadedModules")

        OutputStreamWriter(file).use {
            it.write("package at.hannibal2.skyhanni.skyhannimodule\n\n")
            it.write("object LoadedModules {\n")
            it.write("    val isDev: Boolean = at.hannibal2.skyhanni.utils.system.PlatformUtils.isDevEnvironment\n")
            it.write("    val modules: List<Any> = buildList {\n")

            symbols.forEach { symbol ->
                if (isDevAnnotation(symbol)) {
                    it.write("        if (isDev) add(${symbol.qualifiedName!!.asString()})\n")
                } else {
                    it.write("        add(${symbol.qualifiedName!!.asString()})\n")
                }
            }

            it.write("    }\n")
            it.write("}\n")
        }

        logger.warn("Generated LoadedModules file with ${symbols.size} modules")
    }
}
