package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvents
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigGuiManager.openConfigGui
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.PetDataStorage
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.JacobContestsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.KnownFeaturesJson
import at.hannibal2.skyhanni.data.jsonobjects.local.VisualWordsJson
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.events.utils.PreInitFinishedEvent
import at.hannibal2.skyhanni.features.garden.FarmingStatusTracker
import at.hannibal2.skyhanni.skyhannimodule.LoadedModules
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter
import at.hannibal2.skyhanni.utils.VersionConstants
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@SkyHanniModule
object SkyHanniMod {

    fun preInit() {
        PlatformUtils.checkIfNeuIsLoaded()

        LoadedModules.modules.forEach { SkyHanniModLoader.loadModule(it) }

        SkyHanniEvents.init(modules)

        PreInitFinishedEvent.post()
    }

    fun init() {
        configManager = ConfigManager()
        configManager.firstLoad()
        if (!PlatformUtils.isNeuLoaded()) EnoughUpdatesManager.downloadRepo()
        MinecraftConsoleFilter.initLogging()
        Runtime.getRuntime().addShutdownHook(
            Thread { configManager.saveConfig(ConfigFileType.FEATURES, "shutdown-hook") },
        )
        Runtime.getRuntime().addShutdownHook(
            Thread {
                if (feature.garden.tracking.enabled) {
                    FarmingStatusTracker.prepareAndSendEmbed("Offline")
                }
            },
        )
        try {
            RepoManager.initRepo()
        } catch (e: Exception) {
            Exception("Error reading repo data", e).printStackTrace()
        }
    }

    @HandleEvent
    fun onTick() {
        screenToOpen?.let {
            screenTicks++
            if (screenTicks == 5) {
                val title = InventoryUtils.openInventoryName()
                if (shouldCloseScreen) {
                    //#if MC < 1.21
                    MinecraftCompat.localPlayer.closeScreen()
                    //#else
                    //$$ MinecraftCompat.localPlayer.closeHandledScreen()
                    //#endif
                    OtherInventoryData.close(title)
                }
                shouldCloseScreen = true
                Minecraft.getMinecraft().displayGuiScreen(it)
                screenTicks = 0
                screenToOpen = null
            }
        }
    }

    const val MODID: String = "skyhanni"
    const val VERSION: String = VersionConstants.MOD_VERSION

    val modVersion: ModVersion = ModVersion.fromString(VERSION)

    val isBetaVersion: Boolean
        get() = modVersion.isBeta

    @JvmField
    var feature: Features = Features()
    lateinit var sackData: SackData
    lateinit var friendsData: FriendsJson
    lateinit var knownFeaturesData: KnownFeaturesJson
    lateinit var jacobContestsData: JacobContestsJson
    lateinit var visualWordsData: VisualWordsJson
    lateinit var petData: PetDataStorage

    lateinit var configManager: ConfigManager
    val logger: Logger = LogManager.getLogger("SkyHanni")
    fun getLogger(name: String): Logger {
        return LogManager.getLogger("SkyHanni.$name")
    }

    val modules: MutableList<Any> = ArrayList()
    private val globalJob: Job = Job(null)
    val coroutineScope = CoroutineScope(
        CoroutineName("SkyHanni") + SupervisorJob(globalJob),
    )

    fun launchIOCoroutine(block: suspend CoroutineScope.() -> Unit) {
        launchCoroutine {
            withContext(Dispatchers.IO) {
                block()
            }
        }
    }

    var screenToOpen: GuiScreen? = null
    var shouldCloseScreen: Boolean = true
    private var screenTicks = 0
    fun consoleLog(message: String) {
        logger.log(Level.INFO, message)
    }

    fun launchCoroutine(function: suspend () -> Unit) {
        coroutineScope.launch {
            try {
                function()
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e,
                    e.message ?: "Asynchronous exception caught",
                )
            }
        }
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
