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
import net.minecraftforge.fml.common.ModContainer
//#if MC < 1.16
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.utils.DelayedRun
import kotlin.time.Duration.Companion.INFINITE
import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
//#elseif FORGE
//$$ import net.minecraftforge.fml.loading.FMLEnvironment
//#else
//$$ import net.fabricmc.loader.api.FabricLoader
//$$ import kotlin.system.exitProcess
//#endif

/**
 * This object contains utilities for all platform specific operations.
 * i.e. operations that are specific to the mod loader or the environment the mod is running in.
 */
@SkyHanniModule
object PlatformUtils {

    //#if MC < 1.21
    val MC_VERSION: String = VersionConstants.MC_VERSION
    //#else
    //$$ val MC_VERSION: String = net.minecraft.SharedConstants.getGameVersion().name
    //#endif
    val IS_LEGACY: Boolean = VersionConstants.MC_VERSION == "1.8.9"

    val isDevEnvironment: Boolean by lazy {
        //#if MC < 1.16
        Launch.blackboard?.get("fml.deobfuscatedEnvironment") as? Boolean ?: true
        //#elseif FORGE
        //$$ FMLEnvironment.production.not()
        //#else
        //$$ FabricLoader.getInstance().isDevelopmentEnvironment
        //#endif
    }

    private val allowedFabricReports = setOf(
        "fabricloader",
        "fabric-api"
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        if (validNeuInstalled) return
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
        //#if MC < 1.16
        Loader.instance().modList.forEach {
            add(ModInstance(it.modId, it.name, it.version, it.source.name))
        }
        //#else
        //$$ FabricLoader.getInstance().allMods.forEach {
        //$$    if (it.origin.toString().contains(":META-INF")) return@forEach
        //$$    val origin = it.origin.toString().substringAfterLast('\\')
        //$$    add(ModInstance(it.metadata.id, it.metadata.name, it.metadata.version.toString(), origin))
        //$$ }
        //#endif
    }

    fun shutdownMinecraft(reason: String? = null) {
        val reasonLine = reason?.let { " Reason: $it" }.orEmpty()
        System.err.println("SkyHanni-${VersionConstants.MOD_VERSION} ${"forced the game to shutdown.$reasonLine"}")

        //#if FORGE
        FMLCommonHandler.instance().handleExit(-1)
        //#else
        //$$ exitProcess(-1)
        //#endif
    }

    //#if MC < 1.16
    private val modPackages: Map<String, ModContainer> by lazy {
        Loader.instance().modList.flatMap { mod -> mod.ownedPackages.map { it to mod } }.toMap()
    }

    private fun getModFromPackage(packageName: String?): ModInstance? = modPackages[packageName]?.let {
        ModInstance(it.modId, it.name, it.version, it.source.name)
    }
    //#else
    //$$ private fun getModFromPackage(packageName: String?): ModInstance? {
    //$$    packageName ?: return null
    //$$    if (packageName.startsWith("at.hannibal2.skyhanni")) return ModInstance("skyhanni", "SkyHanni", VersionConstants.MOD_VERSION, "")
    //$$    return null
    //$$ }
    //#endif

    fun Class<*>.getModInstance(): ModInstance? = getModFromPackage(canonicalName?.substringBeforeLast('.'))

    fun isModInstalled(modId: String): Boolean {
        //#if FORGE
        return Loader.isModLoaded(modId)
        //#else
        //$$ return FabricLoader.getInstance().isModLoaded(modId)
        //#endif
    }

    private var validNeuInstalled = false

    fun isNeuLoaded() = validNeuInstalled

    @JvmStatic
    fun checkIfNeuIsLoaded() {
        //#if MC < 1.16
        try {
            Class.forName("io.github.moulberry.notenoughupdates.NotEnoughUpdates")
        } catch (e: Throwable) {
            return
        }

        try {
            val clazz = Class.forName("io.github.moulberry.notenoughupdates.util.ItemResolutionQuery")

            for (field in clazz.methods) {
                if (field.name == "findInternalNameByDisplayName") {
                    validNeuInstalled = true
                    return
                }
            }
        } catch (_: Throwable) {
        }

        val text = listOf(
            "§c§lOutdated NotEnoughUpdates version detected!",
            "§cWhile Skyhanni doesn't require NotEnoughUpdates to function anymore,",
            "§cif you choose to still use NotEnoughUpdates, which is recommended,",
            "§cwe require you to use a newer version of NotEnoughUpdates to ensure",
            "§ccompatibility with some of our features.",
            "§cPlease update NotEnoughUpdates",
        )
        DelayedRun.runNextTick { NotificationManager.queueNotification(SkyHanniNotification(text, INFINITE, true)) }
        //#endif
    }

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
}

data class ModInstance(val id: String, val name: String, val version: String, val sourceJar: String)
