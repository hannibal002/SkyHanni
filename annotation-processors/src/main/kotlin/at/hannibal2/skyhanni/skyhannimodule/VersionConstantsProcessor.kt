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
            it.write("package at.hannibal2.skyhanni.utils\n\n")
            it.write("object VersionConstants {\n")
            it.write("    const val MOD_VERSION = \"$modVersion\"\n")
            it.write("    // Do not use this mc version as its reflective of the compile time version\n")
            it.write("    // And might not be correct at run time\n")
            it.write("    // We use it for the auto updater only\n")
            it.write("    var MC_VERSION = \"$mcVersion\"\n")
            it.write("        private set\n")
            it.write("}\n")
        }
        logger.warn("Generated VersionConstants file with mod version $modVersion and mc version $mcVersion")
        return emptyList()
    }
}
