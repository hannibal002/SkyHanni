package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import java.io.File
import java.io.OutputStreamWriter

// Both annotations live in the main source set rather than here, so they can only be matched by name.
private const val SKYHANNI_MODULE = "at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule"
private const val HANDLE_EVENT = "at.hannibal2.skyhanni.api.event.HandleEvent"

class ModuleProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    modVersion: String,
    private val mcVersion: String,
    private val buildPaths: String?,
    cacheDir: String?,
) : BaseProcessor(codeGenerator, logger, modVersion) {

    private var skyHanniEvent: KSType? = null

    private val cache = KspIncrementalCache(cacheDir, mcVersion, "ksp-module-state")

    private fun Int.withPlural(string: String) = "$this $string${if (this == 1) "" else "s"}"

    override fun processSymbols(resolver: Resolver): List<KSAnnotated> {
        skyHanniEvent = resolver.getClassDeclarationByName(
            "at.hannibal2.skyhanni.api.event.SkyHanniEvent",
        )?.asStarProjectedType()

        val symbols = processBuildPaths(
            resolver.getSymbolsWithAnnotation(SKYHANNI_MODULE).toList(),
        )
        val primaryFunctionNames = resolver.getSymbolsWithAnnotation(PrimaryFunction::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { symbol ->
                val annotation = symbol.annotations.firstOrNull { it.shortName.asString() == "PrimaryFunction" }
                annotation?.arguments?.firstOrNull()?.value as? String
            }
            .toSet()

        validateModuleMembership(resolver)

        val filePaths = symbols.mapNotNull { it.containingFile?.filePath }.toSet()
        val outputFile = cache.outputFile("at/hannibal2/skyhanni/skyhannimodule", "LoadedModules")
        val dirtyFilePaths = cache.evaluate(filePaths, outputFile)

        val dirtyCount = symbols.count { it.containingFile?.filePath in (dirtyFilePaths ?: emptySet()) }
        val cachedCount = symbols.size - dirtyCount
        println(
            "Found ${symbols.size.withPlural("symbol")} with @SkyHanniModule for Minecraft $mcVersion " +
                "($dirtyCount revalidated, $cachedCount from cache)",
        )

        if (dirtyFilePaths == null) {
            logger.warn("No @SkyHanniModule files changed, skipping LoadedModules regeneration")
            cache.commit()
            return emptyList()
        }

        if (dirtyFilePaths.isEmpty()) {
            println("No @SkyHanniModule files changed but LoadedModules.kt is missing, regenerating")
        }

        val validSymbols = symbols.mapNotNull {
            validateSymbol(it, it.containingFile?.filePath in dirtyFilePaths, primaryFunctionNames)
        }
        if (validSymbols.isNotEmpty()) generateFile(validSymbols)
        cache.commit()
        return emptyList()
    }

    private fun <T : KSAnnotated> processBuildPaths(symbols: List<T>): List<T> {
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
     * Validates that every `@HandleEvent` function is declared directly inside a `@SkyHanniModule` class.
     *
     * Handlers declared anywhere else are never registered, because `SkyHanniEvents.register` only reads
     * `declaredMethods` of the objects listed in the generated `LoadedModules`, making them dead code.
     *
     * This runs on every build and ignores [KspIncrementalCache], because files that contain `@HandleEvent`
     * but no `@SkyHanniModule` are never tracked by the cache and could therefore never be reported.
     */
    private fun validateModuleMembership(resolver: Resolver) {
        val functions = processBuildPaths(
            resolver.getSymbolsWithAnnotation(HANDLE_EVENT)
                .filterIsInstance<KSFunctionDeclaration>()
                .toList(),
        )
        for (function in functions) {
            val parent = function.parentDeclaration as? KSClassDeclaration
            if (parent?.annotations?.any { it.shortName.asString() == "SkyHanniModule" } == true) continue
            val name = (function.qualifiedName ?: function.simpleName).asString()
            logger.error(
                "Function $name must be declared directly inside a class annotated with @SkyHanniModule " +
                    "because it is annotated with @HandleEvent, otherwise it is never registered",
                function,
            )
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
            logger.error("@SkyHanniModule is only valid on Kotlin objects", symbol)
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
                            logger.error(
                                "Function $name must have an event parameter, a primary function " +
                                    "name, or an explicit event specification because it is " +
                                    "annotated with @HandleEvent",
                                function,
                            )
                        }

                        1 -> if (eventParameterType == null || !event.isAssignableFrom(eventParameterType)) {
                            logger.error(
                                "Function $name must have an event assignable from SkyHanniEvent " +
                                    "because it is annotated with @HandleEvent",
                                function,
                            )
                        }

                        else -> logger.error(
                            "Function $name has too many parameters. It must have exactly one " +
                                "event parameter, or be parameterless with a primary function " +
                                "name or an explicit event specification because it is annotated " +
                                "with @HandleEvent",
                            function,
                        )
                    }
                }
            }
        }

        return symbol
    }

    private fun KSAnnotation.hasExplicitEventSpec(): Boolean {
        val annotationFilePath = (location as? FileLocation)?.filePath ?: return false
        return arguments.any { argument ->
            val value = argument.value
            (argument.location as? FileLocation)?.filePath == annotationFilePath &&
                when (value) {
                    is KSType -> true
                    is List<*> -> value.isNotEmpty() && value.all { it is KSType }
                    else -> false
                }
        }
    }

    private fun isDevOnly(klass: KSClassDeclaration): Boolean =
        klass.annotations.find { it.shortName.asString() == "SkyHanniModule" }
            ?.arguments?.find { it.name?.asString() == "devOnly" }?.value as? Boolean ?: false

    private fun generateFile(symbols: List<KSClassDeclaration>) {
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
        println("Generated LoadedModules file with ${symbols.size.withPlural("module")}")
    }
}
