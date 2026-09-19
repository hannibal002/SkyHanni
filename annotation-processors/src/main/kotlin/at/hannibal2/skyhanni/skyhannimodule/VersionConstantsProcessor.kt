package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.OutputStreamWriter

class VersionConstantsProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    modVersion: String,
    private val mcVersion: String,
) : BaseProcessor(codeGenerator, logger, modVersion) {

    override fun processSymbols(resolver: Resolver): List<KSAnnotated> {
        val file = codeGenerator.createNewFile(Dependencies(false), "at.hannibal2.skyhanni.utils", "VersionConstants")
        OutputStreamWriter(file).use {
            it.appendLine(
                """
                |package at.hannibal2.skyhanni.utils
                |
                |object VersionConstants {
                |    const val MOD_VERSION = "$modVersion"
                |    /**
                |     * Do not use this Minecraft version as it is reflective of the compile-time version
                |     * and might not be correct at runtime. We use it for update checks only.
                |     */
                |    internal const val MC_VERSION = "$mcVersion"
                |}
                """.trimMargin(),
            )
        }
        println("Generated VersionConstants file with mod version $modVersion and Minecraft version $mcVersion")
        return emptyList()
    }
}
