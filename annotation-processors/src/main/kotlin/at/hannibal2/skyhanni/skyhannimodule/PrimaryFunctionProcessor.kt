package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.OutputStreamWriter

class PrimaryFunctionProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    modVersion: String,
    mcVersion: String,
    cacheDir: String?,
) : BaseProcessor(codeGenerator, logger, modVersion) {

    private val cache = KspIncrementalCache(cacheDir, mcVersion, "ksp-primary-function-state")

    override fun processSymbols(resolver: Resolver): List<KSAnnotated> {
        val skyHanniEvent = resolver.getClassDeclarationByName("at.hannibal2.skyhanni.api.event.SkyHanniEvent")
            ?.asStarProjectedType() ?: return emptyList()

        val symbols = resolver.getSymbolsWithAnnotation(PrimaryFunction::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { skyHanniEvent.isAssignableFrom(it.asStarProjectedType()) }
            .toList()

        val filePaths = symbols.mapNotNull { it.containingFile?.filePath }.toSet()
        val outputFile = cache.outputFile("at/hannibal2/skyhanni/api/event", "GeneratedEventPrimaryFunctionNames")
        val dirtyFilePaths = cache.evaluate(filePaths, outputFile)

        if (dirtyFilePaths == null) {
            logger.warn("No @PrimaryFunction files changed, skipping GeneratedEventPrimaryFunctionNames regeneration")
            cache.commit()
            return emptyList()
        }

        if (dirtyFilePaths.isEmpty()) {
            logger.warn("No @PrimaryFunction files changed but GeneratedEventPrimaryFunctionNames.kt is missing, regenerating")
        }

        generate(symbols)
        cache.commit()
        return emptyList()
    }

    private fun generate(symbols: List<KSClassDeclaration>) {
        val entries = symbols.mapNotNull { symbol ->
            val annotation = symbol.annotations.firstOrNull { it.shortName.asString() == "PrimaryFunction" }
            val value = annotation?.arguments?.firstOrNull()?.value as? String ?: return@mapNotNull null
            val fqName = symbol.qualifiedName?.asString() ?: return@mapNotNull null
            "\"$value\" to $fqName::class.java"
        }.joinToString(",\n        ")

        val dependencies = Dependencies(true, *symbols.mapNotNull { it.containingFile }.toTypedArray())
        val file = codeGenerator.createNewFile(dependencies, "at.hannibal2.skyhanni.api.event", "GeneratedEventPrimaryFunctionNames")
        OutputStreamWriter(file).use {
            it.write("package at.hannibal2.skyhanni.api.event\n\n")
            it.write("object GeneratedEventPrimaryFunctionNames {\n")
            it.write("    val map: Map<String, Class<out SkyHanniEvent>> = mapOf(\n")
            it.write("        $entries\n")
            it.write("    )\n")
            it.write("}\n")
        }
        logger.warn("Generated GeneratedEventPrimaryFunctionNames with ${symbols.size} entries")
    }
}
