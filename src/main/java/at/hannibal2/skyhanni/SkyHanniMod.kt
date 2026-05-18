package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvents
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigGuiManager.openConfigGui
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.SkyHanniConfig
import at.hannibal2.skyhanni.config.StorageData
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.storage.AchievementStorage
import at.hannibal2.skyhanni.config.storage.CustomTodosStorage
import at.hannibal2.skyhanni.config.storage.OrderedWaypointsRoutes
import at.hannibal2.skyhanni.config.storage.SpecificSeaCreatureStorage
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.PetDataStorage
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.JacobContestsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.KnownFeaturesJson
import at.hannibal2.skyhanni.data.jsonobjects.local.VisualWordsJson
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.events.utils.InitFinishedEvent
import at.hannibal2.skyhanni.events.utils.PreInitFinishedEvent
import at.hannibal2.skyhanni.skyhannimodule.LoadedModules
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter
import at.hannibal2.skyhanni.utils.VersionConstants
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.coroutines.CompatCoroutineManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.coroutines.SkyHanniCoroutineManager
import at.hannibal2.skyhanni.utils.render.item.SkyHanniItemRenderCoordinator
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@SkyHanniModule
object SkyHanniMod : CompatCoroutineManager by SkyHanniCoroutineManager(
    CoroutineScope(CoroutineName("SkyHanni") + SupervisorJob(Job(null))),
) {
    fun preInit() {
        LoadedModules.modules.forEach { SkyHanniModLoader.loadModule(it) }

        SkyHanniEvents.init(modules)

        PreInitFinishedEvent.post()
    }

    fun init() {
        configManager = ConfigManager()
        configManager.firstLoad()
        if (PlatformUtils.getRepoPatternDumpLocation() == null) EnoughUpdatesRepoManager.initRepo()
        MinecraftConsoleFilter.initLogging()
        try {
            if (PlatformUtils.getRepoPatternDumpLocation() == null) SkyHanniRepoManager.initRepo()
        } catch (e: Exception) {
            Exception("Error reading repo data", e).printStackTrace()
        }
        InitFinishedEvent.post()
    }

    fun CoroutineSettings.launch(block: suspend CoroutineScope.() -> Unit): Job =
        with(SkyHanniMod) { launchCoroutine(block) }

    fun CoroutineSettings.launchUnScoped(block: suspend () -> Unit): Job =
        with(SkyHanniMod) { launchUnScopedCoroutine(block) }

    fun <T> CoroutineSettings.async(block: suspend CoroutineScope.() -> T): Deferred<T?> =
        with(SkyHanniMod) { asyncCoroutine(block) }

    fun <T> CoroutineSettings.asyncUnScoped(block: suspend () -> T): Deferred<T?> =
        with(SkyHanniMod) { asyncUnScopedCoroutine(block) }

    @HandleEvent
    fun onTick() {
        val screenToOpen = screenToOpen ?: return
        screenTicks++
        if (screenTicks != 5) return
        val title = InventoryUtils.openInventoryName()
        if (shouldCloseScreen) {
            MinecraftCompat.localPlayer.closeContainer()
            OtherInventoryData.close(title)
        }
        shouldCloseScreen = true
        Minecraft.getInstance().setScreen(screenToOpen)
        screenTicks = 0
        this.screenToOpen = null
    }

    @HandleEvent
    fun onClientShutdown() {
        configManager.saveConfig(ConfigFileType.FEATURES, "shutdown-hook")
    }

    @HandleEvent
    fun onRenderShutdown() {
        SkyHanniItemRenderCoordinator.closeAtlas()
    }

    const val MODID: String = "skyhanni"
    const val VERSION: String = VersionConstants.MOD_VERSION

    val modVersion: ModVersion = ModVersion.fromString(VERSION)

    val isBetaVersion: Boolean
        get() = modVersion.isBeta

    val userAgent: String = "SkyHanni/${VERSION}-${PlatformUtils.MC_VERSION}"

    // TODO rename to config. whoever does this, have fun with 644 lines changed
    @JvmField
    var feature: SkyHanniConfig = SkyHanniConfig()
    lateinit var sackData: SackData
    lateinit var storageData: StorageData
    lateinit var friendsData: FriendsJson
    lateinit var knownFeaturesData: KnownFeaturesJson
    lateinit var jacobContestsData: JacobContestsJson
    lateinit var visualWordsData: VisualWordsJson
    lateinit var petData: PetDataStorage
    lateinit var orderedWaypointsRoutesData: OrderedWaypointsRoutes
    lateinit var customTodos: CustomTodosStorage
    lateinit var seaCreatureStorage: SpecificSeaCreatureStorage
    lateinit var achievementStorage: AchievementStorage

    lateinit var configManager: ConfigManager
    val logger: Logger = LogManager.getLogger("SkyHanni")

    val modules: MutableList<Any> = ArrayList()

    var screenToOpen: Screen? = null
    var shouldCloseScreen: Boolean = true
    private var screenTicks = 0
    fun consoleLog(message: String) {
        logger.log(Level.INFO, message)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("sh") {
            aliases = listOf("skyhanni")
            description = "Opens the main SkyHanni config"
            literalCallback("gui") {
                GuiEditManager.openGuiPositionEditor(hotkeyReminder = true)
            }
            argCallback("search", BrigadierArguments.greedyString()) { search ->
                openConfigGui(search)
            }
            simpleCallback {
                openConfigGui()
            }
        }
        event.registerBrigadier("shconfigsave") {
            description = "Manually saving the config"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                ChatUtils.chat("Manually saved the config!")
                configManager.saveConfig(ConfigFileType.FEATURES, "manual-command")
            }
        }
    }
}
