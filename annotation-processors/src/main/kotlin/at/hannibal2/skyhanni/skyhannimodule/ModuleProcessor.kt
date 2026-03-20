package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import java.io.File
import java.io.OutputStreamWriter

class ModuleProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    modVersion: String,
    private val mcVersion: String,
    private val buildPaths: String?,
) : BaseProcessor(codeGenerator, logger, modVersion) {

    private var skyHanniEvent: KSType? = null
    private var minecraftForgeEvent: KSType? = null
    private val warnings = mutableListOf<String>()

    override fun processSymbols(resolver: Resolver): List<KSAnnotated> {
        skyHanniEvent = resolver.getClassDeclarationByName("at.hannibal2.skyhanni.api.event.SkyHanniEvent")?.asStarProjectedType()

        if (mcVersion == "1.8.9") {
            minecraftForgeEvent = resolver.getClassDeclarationByName("net.minecraftforge.fml.common.eventhandler.Event")
                ?.asStarProjectedType() ?: return emptyList()
        }

        val symbols = processBuildPaths(resolver.getSymbolsWithAnnotation(SkyHanniModule::class.qualifiedName!!).toList())
        logger.warn("Found ${symbols.size} symbols with @SkyHanniModule for mc $mcVersion")
        val validSymbols = symbols.mapNotNull { validateSymbol(it) }

        if (validSymbols.isNotEmpty()) generateFile(validSymbols)
        return emptyList()
    }

    private fun processBuildPaths(symbols: List<KSAnnotated>): List<KSAnnotated> {
        val buildPathsFile = buildPaths?.let { File(it) }?.takeIf { it.exists() } ?: return symbols
        val validPaths = buildPathsFile.readText().lineSequence()
            .map { it.substringBefore("#").replace(Regex("\\.(?!kt|java|\\()"), "/").trim() }
            .filter { it.isNotBlank() }
            .toSet()
        return symbols.filter {
            val path = it.containingFile?.filePath ?: return@filter false
            path.substringAfter("/main/java/") !in validPaths
        }
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

        val className = symbol.qualifiedName?.asString() ?: "unknown"
        for (function in symbol.getDeclaredFunctions()) {
            if (mcVersion == "1.8.9" && function.annotations.any { it.shortName.asString() == "SubscribeEvent" }) {
                val firstParam = function.parameters.firstOrNull()?.type?.resolve()!!
                if (!minecraftForgeEvent!!.isAssignableFrom(firstParam))
                    warnings.add("Function in $className must have an event assignable from $minecraftForgeEvent because it is annotated with @SubscribeEvent")
            }
            if (function.annotations.any { it.shortName.asString() == "HandleEvent" }) {
                val firstParam = function.parameters.firstOrNull()?.type?.resolve()
                val eventType = function.annotations.find { it.shortName.asString() == "HandleEvent" }
                    ?.arguments?.find { it.name?.asString() == "eventType" }?.value
                if ((firstParam == null && eventType == null) || (firstParam != null && !skyHanniEvent!!.isAssignableFrom(firstParam)))
                    warnings.add("Function in $className must have an event assignable from $skyHanniEvent because it is annotated with @HandleEvent")
            }
        }
        return symbol
    }

    private fun isDevOnly(klass: KSClassDeclaration): Boolean =
        klass.annotations.find { it.shortName.asString() == "SkyHanniModule" }
            ?.arguments?.find { it.name?.asString() == "devOnly" }?.value as? Boolean ?: false

    private fun generateFile(symbols: List<KSClassDeclaration>) {
        if (warnings.isNotEmpty()) {
            warnings.forEach { logger.warn(it) }
            error("${warnings.size} errors related to event annotations found, please fix them before continuing. Click on the kspKotlin build log for more information.")
        }

        val sources = symbols.mapNotNull { it.containingFile }.toTypedArray()
        val file = codeGenerator.createNewFile(Dependencies(true, *sources), "at.hannibal2.skyhanni.skyhannimodule", "LoadedModules")
        OutputStreamWriter(file).use {
            it.write("package at.hannibal2.skyhanni.skyhannimodule\n\n")
            it.write("@Suppress(\"LargeClass\")\n")
            it.write("object LoadedModules {\n")
            it.write("    val isDev: Boolean = at.hannibal2.skyhanni.utils.system.PlatformUtils.isDevEnvironment\n")
            it.write("    val modules: List<Any> = buildList {\n")
            symbols.forEach { symbol ->
                if (isDevOnly(symbol)) {
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
