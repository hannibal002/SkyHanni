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
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.FileLocation
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

    private fun Int.withPlural(string: String) = "$this $string${if (this == 1) "" else "s"}"

    override fun processSymbols(resolver: Resolver): List<KSAnnotated> {
        skyHanniEvent = resolver.getClassDeclarationByName(
            "at.hannibal2.skyhanni.api.event.SkyHanniEvent",
        )?.asStarProjectedType()

        val symbols = processBuildPaths(
            resolver.getSymbolsWithAnnotation(SkyHanniModule::class.qualifiedName!!).toList(),
        )
        val primaryFunctionNames = resolver.getSymbolsWithAnnotation(PrimaryFunction::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { symbol ->
                val annotation = symbol.annotations.firstOrNull { it.shortName.asString() == "PrimaryFunction" }
                annotation?.arguments?.firstOrNull()?.value as? String
            }
            .toSet()

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
        logger.warn(
            "Found ${symbols.size.withPlural("symbol")} with @SkyHanniModule for mc $mcVersion " +
                "($dirtyCount revalidated, $cachedCount from cache)",
        )

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

        val validSymbols = symbols.mapNotNull {
            validateSymbol(it, it.containingFile?.filePath in dirtyFilePaths, primaryFunctionNames)
        }
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
        file.writeText(
            states.entries.joinToString("\n") { (path, state) ->
                "$path|${state.mtime}|${state.crc}"
            },
        )
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
    private fun validateSymbol(
        symbol: KSAnnotated,
        isDirty: Boolean,
        primaryFunctionNames: Set<String>,
    ): KSClassDeclaration? {
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
            for (function in symbol.getDeclaredFunctions()) {
                if (function.annotations.any { it.shortName.asString() == "HandleEvent" }) {
                    val event = skyHanniEvent ?: return symbol
                    val handleEvent = function.annotations.find { it.shortName.asString() == "HandleEvent" }
                    val eventParameterType = function.extensionReceiver?.resolve()
                        ?: function.parameters.firstOrNull()?.type?.resolve()
                    val parameterCount = function.parameters.size + if (function.extensionReceiver != null) 1 else 0
                    val hasPrimaryFunction = function.simpleName.asString() in primaryFunctionNames
                    val hasExplicitEventSpec = handleEvent?.hasExplicitEventSpec() == true
                    val name = (function.qualifiedName ?: function.simpleName).asString()

                    when (parameterCount) {
                        0 -> if (!hasPrimaryFunction && !hasExplicitEventSpec) {
                            warnings.add(
                                "Function $name must have an event parameter, a primary function " +
                                    "name, or an explicit event specification because it is " +
                                    "annotated with @HandleEvent",
                            )
                        }

                        1 -> if (eventParameterType == null || !event.isAssignableFrom(eventParameterType)) {
                            warnings.add(
                                "Function $name must have an event assignable from $event " +
                                    "because it is annotated with @HandleEvent",
                            )
                        }

                        else -> warnings.add(
                            "Function $name has too many parameters. It must have exactly one " +
                                "event parameter, or be parameterless with a primary function " +
                                "name or an explicit event specification because it is annotated " +
                                "with @HandleEvent",
                        )
                    }
                }
            }
        }

        return symbol
    }

    private fun KSAnnotation.hasExplicitEventSpec(): Boolean {
        val location = location as? FileLocation ?: return false
        val file = File(location.filePath)
        if (!file.exists()) return false

        val lines = file.readLines()
        val startIndex = (location.lineNumber - 1).coerceAtLeast(0)
        val endIndex = minOf(lines.size, startIndex + 12)
        val snippet = buildString {
            for (index in startIndex until endIndex) {
                append(lines[index])
                append('\n')
                if (lines[index].contains("fun ")) break
            }
        }

        val annotationText = snippet.substringAfter("@HandleEvent", "")
        return annotationText.contains("::class")
    }

    private fun isDevOnly(klass: KSClassDeclaration): Boolean =
        klass.annotations.find { it.shortName.asString() == "SkyHanniModule" }
            ?.arguments?.find { it.name?.asString() == "devOnly" }?.value as? Boolean ?: false

    private fun generateFile(symbols: List<KSClassDeclaration>) {
        if (warnings.isNotEmpty()) {
            warnings.forEach { logger.warn(it) }
            error(
                "${warnings.size.withPlural("error")} related to event annotations found, please " +
                    "fix them before continuing. Click on the kspKotlin build log for more " +
                    "information.",
            )
        }

        val sources = symbols.mapNotNull { it.containingFile }.toTypedArray()
        val file = codeGenerator.createNewFile(
            Dependencies(true, *sources),
            "at.hannibal2.skyhanni.skyhannimodule",
            "LoadedModules",
        )
        OutputStreamWriter(file).use {
            it.appendLine(
                """
                |package at.hannibal2.skyhanni.skyhannimodule
                |
                |@Suppress("LargeClass")
                |object LoadedModules {
                |    val isDev: Boolean = at.hannibal2.skyhanni.utils.system.PlatformUtils.isDevEnvironment
                |    val modules: List<Any> = buildList {
            """.trimMargin(),
            )
            symbols.forEach { symbol ->
                val prefix = if (isDevOnly(symbol)) "if (isDev) " else ""
                it.appendLine("        ${prefix}add(${symbol.qualifiedName!!.asString()})")
            }
            it.appendLine(
                """
                |    }
                |}
            """.trimMargin(),
            )
        }
        logger.info("Generated LoadedModules file with ${symbols.size.withPlural("module")}")
    }
}
