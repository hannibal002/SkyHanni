package at.hannibal2.skyhanni.utils.system

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.LoadedModules
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.MarkdownBuilder
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.VersionConstants
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * This object contains utilities for all platform specific operations.
 * i.e. operations that are specific to the mod loader or the environment the mod is running in.
 */
@SkyHanniModule
object PlatformUtils {
    private val platform: Platform =
        if (runCatching { Class.forName("org.junit.jupiter.api.Test") }.isSuccess) {
            TestPlatform()
        } else {
            FabricPlatform()
        }

    val MC_VERSION: String
        get() = platform.mcVersion

    @JvmStatic
    @get:JvmName("isDevEnvironment")
    val isDevEnvironment: Boolean
        get() = platform.isDevEnvironment

    val gameDir: Path
        get() = platform.gameDir

    val dataDir: Path
        get() = platform.dataDir

    val configDir: Path
        get() = platform.configDir

    val logsDir: Path
        get() = platform.logsDir

    fun shutdownMinecraft(reason: String? = null) =
        platform.shutdownMinecraft(reason)

    fun isModInstalled(modId: String): Boolean =
        platform.isModInstalled(modId)

    fun getLoadedMods(): List<ModInstance> = platform.getLoadedMods()

    private val allowedFabricReports = setOf(
        "fabricloader",
        "fabric-api",
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shmodlist") {
            description = "Get a Discord-formatted list of all loaded mods"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                val loadedMods = getLoadedMods().filter {
                    it.id in allowedFabricReports || !it.id.startsWith("fabric-")
                }
                val loadedModsMd = MarkdownBuilder().category("Mods Loaded")
                loadedMods.forEach { (_, name, version, origin) ->
                    loadedModsMd.append(name, "$origin ($version)")
                }
                OSUtils.copyToClipboard(loadedModsMd.toString())
                ChatUtils.chat("Copied ${loadedMods.size} mods to clipboard!")
            }
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Loaded Mods")
        event.addIrrelevant {
            getLoadedMods().forEach { (_, name, version, origin) ->
                add("$name: \t$origin ($version)")
            }
        }
    }

    private fun getModFromPackage(packageName: String?): ModInstance? {
        packageName ?: return null
        if (packageName.startsWith("at.hannibal2.skyhanni")) return ModInstance("skyhanni", "SkyHanni", VersionConstants.MOD_VERSION, "")
        return null
    }

    fun Class<*>.getModInstance(): ModInstance? = getModFromPackage(canonicalName?.substringBeforeLast('.'))

    fun isMcAbove(version: String): Boolean {
        return MCVersion.fromString(version) > MCVersion.currentMcVersion
    }

    fun isMcAbove(version: MCVersion): Boolean {
        return version > MCVersion.currentMcVersion
    }

    fun isMcBelow(version: String): Boolean {
        return MCVersion.fromString(version) < MCVersion.currentMcVersion
    }

    fun isMcBelow(version: MCVersion): Boolean {
        return version < MCVersion.currentMcVersion
    }

    fun getRepoPatternDumpLocation(): String? {
        if (System.getProperty("SkyHanniDumpRegex.enabled") != "true") return null
        val dumpDirective = System.getProperty("SkyHanniDumpRegex")
        if (dumpDirective.isNullOrBlank()) return null
        return dumpDirective
    }
}
