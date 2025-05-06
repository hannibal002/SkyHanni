package at.hannibal2.skyhanni.utils.system

//#if MC < 1.16
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.VersionConstants
import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModContainer
import kotlin.time.Duration.Companion.INFINITE

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
object PlatformUtils {

    const val MC_VERSION: String = VersionConstants.MC_VERSION

    val isDevEnvironment: Boolean by lazy {
        //#if MC < 1.16
        Launch.blackboard?.get("fml.deobfuscatedEnvironment") as? Boolean ?: true
        //#elseif FORGE
        //$$ FMLEnvironment.production.not()
        //#else
        //$$ FabricLoader.getInstance().isDevelopmentEnvironment
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
        ModInstance(it.modId, it.name, it.version)
    }

    fun Class<*>.getModInstance(): ModInstance? = getModFromPackage(canonicalName?.substringBeforeLast('.'))
    //#else
    //$$ fun Class<*>.getModInstance(): ModInstance? = null
    //#endif

    fun isModInstalled(modId: String): Boolean {
        //#if FORGE
        return Loader.isModLoaded(modId)
        //#else
        // TODO implement this for fabric
        //$$ return false
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
}

data class ModInstance(val id: String, val name: String, val version: String)
