package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

class ModuleProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    // TODO remove once all events are migrated to SkyHanniEvent
    private var skyHanniEvent: KSType? = null
    private var minecraftForgeEvent: KSType? = null

    override fun process(resolver: Resolver): List<KSAnnotated> {

        skyHanniEvent =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("at.hannibal2.skyhanni.api.event.SkyHanniEvent"))
                ?.asStarProjectedType()
        minecraftForgeEvent =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("net.minecraftforge.fml.common.eventhandler.Event"))
                ?.asStarProjectedType()

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

        checkCorrectEventAnnotations(symbol)

        return symbol
    }

    // TODO use Kotlin Poet once KMixins is merged
    private fun generateFile(symbols: List<KSClassDeclaration>) {
        val dependencies = symbols.mapNotNull { it.containingFile }.toTypedArray()
        val deps = Dependencies(true, *dependencies)

        val file = codeGenerator.createNewFile(deps, "at.hannibal2.skyhanni.skyhannimodule", "LoadedModules")

        OutputStreamWriter(file).use {
            it.write("package at.hannibal2.skyhanni.skyhannimodule\n\n")
            it.write("object LoadedModules {\n")
            it.write("    val modules: List<Any> = listOf(\n")

            symbols.forEach { symbol ->
                it.write("        ${symbol.qualifiedName!!.asString()},\n")
            }

            it.write("    )\n")
            it.write("}\n")
        }

        logger.warn("Generated LoadedModules file with ${symbols.size} modules")
    }

    // TODO remove once all events are migrated to SkyHanniEvent
    private fun checkCorrectEventAnnotations(symbol: KSClassDeclaration) {

        for (function in symbol.getDeclaredFunctions()) {
            if (function.annotations.any { it.shortName.asString() == "SubscribeEvent" }) {
                val firstParameter = function.parameters.firstOrNull()?.type?.resolve()
                isDeclarable(firstParameter!!, minecraftForgeEvent!!, "SubscribeEvent")
            }

            if (function.annotations.any { it.shortName.asString() == "HandleEvent" }) {
                val firstParameter = function.parameters.firstOrNull()?.type?.resolve()
                isDeclarable(firstParameter!!, skyHanniEvent!!, "HandleEvent")
            }
        }
    }

    private fun isDeclarable(parameterType: KSType, targetType: KSType, annotationType: String) {
        if (!targetType.isAssignableFrom(parameterType)) {
            error("Function parameter must be assignable from $targetType because it is annotated with @$annotationType")
        }
    }
}
