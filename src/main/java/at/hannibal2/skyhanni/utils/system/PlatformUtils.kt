package at.hannibal2.skyhanni.utils.system

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.MarkdownBuilder
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.VersionConstants
import net.fabricmc.loader.api.FabricLoader
import kotlin.system.exitProcess

/**
 * This object contains utilities for all platform specific operations.
 * i.e. operations that are specific to the mod loader or the environment the mod is running in.
 */
@SkyHanniModule
object PlatformUtils {

    val MC_VERSION: String = net.minecraft.SharedConstants.getCurrentVersion().name
    val IS_LEGACY: Boolean = false

    val isDevEnvironment: Boolean by lazy {
        FabricLoader.getInstance().isDevelopmentEnvironment
    }

    private val allowedFabricReports = setOf(
        "fabricloader",
        "fabric-api",
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shmodlist") {
            description = "Get a Discord-formatted list of all loaded mods"
            category = CommandCategory.USERS_ACTIVE
            callback {
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
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Loaded Mods")
        event.addIrrelevant {
            getLoadedMods().forEach { (_, name, version, origin) ->
                add("$name: \t$origin ($version)")
            }
        }
    }

    private fun getLoadedMods(): List<ModInstance> = buildList {
        FabricLoader.getInstance().allMods.forEach {
            if (it.origin.toString().contains(":META-INF")) return@forEach
            val origin = it.origin.toString().substringAfterLast('\\')
            add(ModInstance(it.metadata.id, it.metadata.name, it.metadata.version.toString(), origin))
        }
    }

    fun shutdownMinecraft(reason: String? = null) {
        val reasonLine = reason?.let { " Reason: $it" }.orEmpty()
        System.err.println("SkyHanni-${VersionConstants.MOD_VERSION} ${"forced the game to shutdown.$reasonLine"}")

        exitProcess(-1)
    }

    private fun getModFromPackage(packageName: String?): ModInstance? {
        packageName ?: return null
        if (packageName.startsWith("at.hannibal2.skyhanni")) return ModInstance("skyhanni", "SkyHanni", VersionConstants.MOD_VERSION, "")
        return null
    }

    fun Class<*>.getModInstance(): ModInstance? = getModFromPackage(canonicalName?.substringBeforeLast('.'))

    fun isModInstalled(modId: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }

    fun isNeuLoaded() = false

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

data class ModInstance(val id: String, val name: String, val version: String, val sourceJar: String)
