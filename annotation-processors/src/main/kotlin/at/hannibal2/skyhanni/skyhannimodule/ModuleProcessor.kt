package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getClassDeclarationByName
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
import java.io.File
import java.io.OutputStreamWriter

class ModuleProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val modVersion: String,
    private val mcVersion: String,
    private val buildPaths: String?,
) : SymbolProcessor {

    companion object {
        private val processedVersions = mutableSetOf<String>()
    }

    private var skyHanniEvent: KSType? = null
    private var minecraftForgeEvent: KSType? = null
    private val warnings = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!processedVersions.add(mcVersion)) {
            return emptyList()
        }
        generateVersionConstants()

        skyHanniEvent =
            resolver.getClassDeclarationByName("at.hannibal2.skyhanni.api.event.SkyHanniEvent")?.asStarProjectedType()

        if (mcVersion == "1.8.9") {
            minecraftForgeEvent = resolver.getClassDeclarationByName("net.minecraftforge.fml.common.eventhandler.Event")
                ?.asStarProjectedType()
                ?: return emptyList()
        }

        val symbols = processBuildPaths(resolver.getSymbolsWithAnnotation(SkyHanniModule::class.qualifiedName!!).toList())
        logger.warn("Found ${symbols.size} symbols with @SkyHanniModule for mc $mcVersion")
        val validSymbols = symbols.mapNotNull { validateSymbol(it) }

        if (validSymbols.isNotEmpty()) {
            generateFile(validSymbols)
        }

        return emptyList()
    }

    private fun processBuildPaths(symbols: List<KSAnnotated>): List<KSAnnotated> {
        val buildPathsFile = buildPaths?.let { File(it) } ?: return symbols
        if (!buildPathsFile.exists()) {
            return symbols
        }

        val validPaths = buildPathsFile.readText().lineSequence()
            .map { it.substringBefore("#").trim() }
            .filter { it.isNotBlank() }
            .toSet()

        return symbols.filter {
            val path = it.containingFile?.filePath ?: return@filter false
            val properPath = path.substringAfter("/main/java/")
            properPath in validPaths
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

        // TODO remove once all events are migrated to SkyHanniEvent
        val className = symbol.qualifiedName?.asString() ?: "unknown"

        for (function in symbol.getDeclaredFunctions()) {
            if (function.annotations.any { it.shortName.asString() == "SubscribeEvent" } && mcVersion == "1.8.9") {
                val firstParameter = function.parameters.firstOrNull()?.type?.resolve()!!
                if (!minecraftForgeEvent!!.isAssignableFrom(firstParameter)) {
                    warnings.add("Function in $className must have an event assignable from $minecraftForgeEvent because it is annotated with @SubscribeEvent")
                }
            }

            if (function.annotations.any { it.shortName.asString() == "HandleEvent" }) {
                val firstParameter = function.parameters.firstOrNull()?.type?.resolve()
                val handleEventAnnotation = function.annotations.find { it.shortName.asString() == "HandleEvent" }
                val eventType = handleEventAnnotation?.arguments?.find { it.name?.asString() == "eventType" }?.value
                val isFirstParameterProblem = firstParameter == null && eventType == null
                val notAssignable = firstParameter != null && !skyHanniEvent!!.isAssignableFrom(firstParameter)

                if (isFirstParameterProblem || notAssignable) {
                    warnings.add("Function in $className must have an event assignable from $skyHanniEvent because it is annotated with @HandleEvent")
                }
            }
        }

        return symbol
    }

    //TODO remove when KMixins added as it contains KSP annotation helpers.
    private fun isDevAnnotation(klass: KSClassDeclaration): Boolean {
        val annotation = klass.annotations.find { it.shortName.asString() == "SkyHanniModule" } ?: return false
        return annotation.arguments.find { it.name?.asString() == "devOnly" }?.value as? Boolean ?: false
    }

    private fun isNeuAnnotation(klass: KSClassDeclaration): Boolean {
        val annotation = klass.annotations.find { it.shortName.asString() == "SkyHanniModule" } ?: return false
        return annotation.arguments.find { it.name?.asString() == "neuRequired" }?.value as? Boolean ?: false
    }

    // TODO use Kotlin Poet once KMixins is merged
    private fun generateFile(symbols: List<KSClassDeclaration>) {

        if (warnings.isNotEmpty()) {
            warnings.forEach { logger.warn(it) }
            error("${warnings.size} errors related to event annotations found, please fix them before continuing. Click on the kspKotlin build log for more information.")
        }

        val sources = symbols.mapNotNull { it.containingFile }.toTypedArray()
        val dependencies = Dependencies(true, *sources)

        val file = codeGenerator.createNewFile(dependencies, "at.hannibal2.skyhanni.skyhannimodule", "LoadedModules")

        OutputStreamWriter(file).use {
            it.write("package at.hannibal2.skyhanni.skyhannimodule\n\n")
            it.write("@Suppress(\"LargeClass\")\n")
            it.write("object LoadedModules {\n")
            it.write("    val isDev: Boolean = at.hannibal2.skyhanni.utils.system.PlatformUtils.isDevEnvironment\n")
            it.write("    val hasNeu: Boolean get() = at.hannibal2.skyhanni.utils.system.PlatformUtils.isNeuLoaded()\n")
            it.write("    val modules: List<Any> = buildList {\n")

            symbols.forEach { symbol ->
                if (isDevAnnotation(symbol)) {
                    it.write("        if (isDev) add(${symbol.qualifiedName!!.asString()})\n")
                } else if (isNeuAnnotation(symbol)) {
                    it.write("        if (hasNeu) add(${symbol.qualifiedName!!.asString()})\n")
                } else {
                    it.write("        add(${symbol.qualifiedName!!.asString()})\n")
                }
            }

            it.write("    }\n")
            it.write("}\n")
        }

        logger.warn("Generated LoadedModules file with ${symbols.size} modules")
    }

    private fun generateVersionConstants() {

        val file = codeGenerator.createNewFile(
            Dependencies(false),
            "at.hannibal2.skyhanni.utils",
            "VersionConstants",
        )

        OutputStreamWriter(file).use {
            it.write("package at.hannibal2.skyhanni.utils\n\n")
            it.write("object VersionConstants {\n")
            it.write("    const val MOD_VERSION = \"$modVersion\"\n")
            it.write("    const val MC_VERSION = \"$mcVersion\"\n")
            it.write("}\n")
        }
        logger.warn("Generated VersionConstants file with mod version $modVersion and mc version $mcVersion")
    }
}
