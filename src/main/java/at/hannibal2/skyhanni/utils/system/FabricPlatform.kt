package at.hannibal2.skyhanni.utils.system

import at.hannibal2.skyhanni.utils.VersionConstants
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import java.nio.file.Path
import kotlin.system.exitProcess

class FabricPlatform : Platform() {
    override val mcVersion: String
        get() = SharedConstants.getCurrentVersion().name()

    override val isDevEnvironment: Boolean
        get() = FabricLoader.getInstance().isDevelopmentEnvironment

    override val gameDir: Path
        get() = FabricLoader.getInstance().gameDir

    override val configDir: Path
        get() = FabricLoader.getInstance().configDir

    override val dataDir: Path =
        gameDir.resolve("data")

    override val logsDir: Path =
        gameDir.resolve("logs")

    override fun getLoadedMods(): List<ModInstance> = buildList {
        FabricLoader.getInstance().allMods.forEach {
            if (it.origin.toString().contains(":META-INF")) return@forEach
            val origin = it.origin.toString().substringAfterLast('\\')
            add(ModInstance(it.metadata.id, it.metadata.name, it.metadata.version.toString(), origin))
        }
    }

    override fun isModInstalled(modId: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }

    override fun shutdownMinecraft(reason: String?) {
        val reasonLine = reason?.let { " Reason: $it" }.orEmpty()
        System.err.println(
            "SkyHanni-${VersionConstants.MOD_VERSION} forced the game to shutdown.$reasonLine"
        )
        exitProcess(-1)
    }
}
