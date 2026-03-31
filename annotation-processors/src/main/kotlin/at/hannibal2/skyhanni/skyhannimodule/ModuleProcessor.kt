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
import java.util.zip.CRC32

class ModuleProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    modVersion: String,
    private val mcVersion: String,
    private val buildPaths: String?,
    cacheDir: String?,
) : BaseProcessor(codeGenerator, logger, modVersion) {

    private var skyHanniEvent: KSType? = null
    private val warnings = mutableListOf<String>()
    private val stateFile: File? = cacheDir?.let { File(it, "ksp-module-state-$mcVersion.txt") }

    private data class FileState(val mtime: Long, val crc: Long)

    override fun processSymbols(resolver: Resolver): List<KSAnnotated> {
        skyHanniEvent = resolver.getClassDeclarationByName("at.hannibal2.skyhanni.api.event.SkyHanniEvent")?.asStarProjectedType()

        val symbols = processBuildPaths(resolver.getSymbolsWithAnnotation(SkyHanniModule::class.qualifiedName!!).toList())

        val cachedStates = readStateFile()
        val newStates = mutableMapOf<String, FileState>()
        val dirtyFilePaths = mutableSetOf<String>()

        for (path in symbols.mapNotNull { it.containingFile?.filePath }.toSet()) {
            val mtime = File(path).lastModified()
            val cached = cachedStates?.get(path)
            if (cached != null && cached.mtime == mtime) {
                newStates[path] = cached
            } else {
                val crc = fileCrc(path)
                newStates[path] = FileState(mtime, crc)
                if (cached == null || cached.crc != crc) dirtyFilePaths.add(path)
            }
        }

        val dirtyCount = symbols.count { it.containingFile?.filePath in dirtyFilePaths }
        val cachedCount = symbols.size - dirtyCount
        logger.warn("Found ${symbols.size} symbols with @SkyHanniModule for mc $mcVersion ($dirtyCount revalidated, $cachedCount from cache)")

        if (dirtyFilePaths.isEmpty()) {
            val outputFile = stateFile?.parentFile?.let {
                File(it, "generated/ksp/main/kotlin/at/hannibal2/skyhanni/skyhannimodule/LoadedModules.kt")
            }
            if (outputFile?.exists() != false) {
                logger.warn("No @SkyHanniModule files changed, skipping LoadedModules regeneration")
                writeStateFile(newStates)
                return emptyList()
            }
            logger.warn("No @SkyHanniModule files changed but LoadedModules.kt is missing, regenerating")
        }

        val validSymbols = symbols.mapNotNull { validateSymbol(it, it.containingFile?.filePath in dirtyFilePaths) }
        if (validSymbols.isNotEmpty()) generateFile(validSymbols)
        writeStateFile(newStates)
        return emptyList()
    }

    private fun fileCrc(path: String): Long {
        val crc = CRC32()
        crc.update(File(path).readBytes())
        return crc.value
    }

    private fun readStateFile(): Map<String, FileState>? {
        val file = stateFile?.takeIf { it.exists() } ?: return null
        return file.readLines().mapNotNull { line ->
            val hashIdx = line.lastIndexOf('|')
            if (hashIdx < 0) return@mapNotNull null
            val mtimeIdx = line.lastIndexOf('|', hashIdx - 1)
            if (mtimeIdx < 0) return@mapNotNull null
            val path = line.substring(0, mtimeIdx)
            val mtime = line.substring(mtimeIdx + 1, hashIdx).toLongOrNull() ?: return@mapNotNull null
            val crc = line.substring(hashIdx + 1).toLongOrNull() ?: return@mapNotNull null
            path to FileState(mtime, crc)
        }.toMap()
    }

    private fun writeStateFile(states: Map<String, FileState>) {
        val file = stateFile ?: return
        file.parentFile?.mkdirs()
        file.writeText(states.entries.joinToString("\n") { (path, state) -> "$path|${state.mtime}|${state.crc}" })
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

    /**
     * Validates that a symbol is a valid `@SkyHanniModule` target.
     *
     * @param symbol The annotated symbol to validate.
     * @param isDirty Whether the symbol's source file is new or modified since the last build.
     *                If false, expensive type resolution is skipped as the symbol was already validated.
     */
    private fun validateSymbol(symbol: KSAnnotated, isDirty: Boolean): KSClassDeclaration? {
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

        if (isDirty) {
            val className = symbol.qualifiedName?.asString() ?: "unknown"
            for (function in symbol.getDeclaredFunctions()) {
                if (function.annotations.any { it.shortName.asString() == "HandleEvent" }) {
                    val event = skyHanniEvent ?: return symbol
                    val firstParam = function.parameters.firstOrNull()?.type?.resolve()
                    val eventType = function.annotations.find { it.shortName.asString() == "HandleEvent" }
                        ?.arguments?.find { it.name?.asString() == "eventType" }?.value
                    if ((firstParam == null && eventType == null) || (firstParam != null && !event.isAssignableFrom(firstParam)))
                        warnings.add("Function in $className must have an event assignable from $event because it is annotated with @HandleEvent")
                }
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
