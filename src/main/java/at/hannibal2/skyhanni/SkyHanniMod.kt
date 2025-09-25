package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvents
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigGuiManager.openConfigGui
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.StorageData
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.storage.OrderedWaypointsRoutes
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
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
        if (!PlatformUtils.isNeuLoaded()) EnoughUpdatesRepoManager.initRepo()
        MinecraftConsoleFilter.initLogging()
        Runtime.getRuntime().addShutdownHook(
            Thread { configManager.saveConfig(ConfigFileType.FEATURES, "shutdown-hook") },
        )
        try {
            SkyHanniRepoManager.initRepo()
        } catch (e: Exception) {
            Exception("Error reading repo data", e).printStackTrace()
            SkyHanniRepoManager.progress.end("Error reading repo data: ${e.message}")
        }
        InitFinishedEvent.post()
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
    lateinit var storageData: StorageData
    lateinit var friendsData: FriendsJson
    lateinit var knownFeaturesData: KnownFeaturesJson
    lateinit var jacobContestsData: JacobContestsJson
    lateinit var visualWordsData: VisualWordsJson
    lateinit var petData: PetDataStorage
    lateinit var orderedWaypointsRoutesData: OrderedWaypointsRoutes

    lateinit var configManager: ConfigManager
    val logger: Logger = LogManager.getLogger("SkyHanni")
    fun getLogger(name: String): Logger {
        return LogManager.getLogger("SkyHanni.$name")
    }

    val modules: MutableList<Any> = ArrayList()
    private val globalJob: Job = Job(null)
    private val coroutineScope = CoroutineScope(
        CoroutineName("SkyHanni") + SupervisorJob(globalJob),
    )

    /**
     * Launch an IO coroutine with a lock on the provided mutex.
     * This coroutine will catch any exceptions thrown by the provided function.
     * @param mutex The mutex to lock during the execution of the block.
     * @param block The suspend function to execute within the IO context.
     */
    fun launchIOCoroutineWithMutex(
        name: String,
        mutex: Mutex,
        timeout: Duration = 10.seconds,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launchCoroutine("launchIOCoroutineWithMutex $name", timeout) {
        mutex.withLock {
            withContext(Dispatchers.IO, block)
        }
    }

    /**
     * Launch an IO coroutine in the SkyHanni scope.
     * This coroutine will catch any exceptions thrown by the provided function.
     * @param block The suspend function to execute within the IO context.
     */
    fun launchIOCoroutine(
        name: String,
        timeout: Duration = 10.seconds,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launchCoroutine("launchIOCoroutine $name", timeout) {
        withContext(Dispatchers.IO, block)
    }

    /**
     * Launches a coroutine in the SkyHanni scope.
     * This coroutine will catch any exceptions thrown by the provided function.
     * The function provided here must not rely on the CoroutineScope's context.
     * @param block The block to execute in the coroutine.
     */
    fun launchNoScopeCoroutine(
        name: String,
        timeout: Duration = 10.seconds,
        block: suspend () -> Unit,
    ): Job = launchCoroutine("launchNoScopeCoroutine $name", timeout) { block() }

    /**
     * Launch a coroutine with a lock on the provided mutex.
     * This coroutine will catch any exceptions thrown by the provided function.
     * @param mutex The mutex to lock during the execution of the block.
     * @param block The suspend function to execute within the IO context.
     */
    fun launchCoroutineWithMutex(
        name: String,
        mutex: Mutex,
        timeout: Duration = 10.seconds,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launchCoroutine("launchCoroutineWithMutex $name", timeout) {
        mutex.withLock { block() }
    }

    /**
     * Launches a coroutine in the SkyHanni scope.
     * This coroutine will catch any exceptions thrown by the provided function.
     * @param function The suspend function to execute in the coroutine.
     */
    @OptIn(InternalCoroutinesApi::class)
    fun launchCoroutine(
        name: String,
        timeout: Duration = 10.seconds,
        function: suspend CoroutineScope.() -> Unit,
    ): Job = coroutineScope.launch {
        val mainJob = launch {
            try {
                function()
            } catch (e: CancellationException) {
                // Don't notify the user about cancellation exceptions - these are to be expected at times
                val jobState = coroutineContext[Job]?.toString() ?: "unknown job"
                val cancellationCause = coroutineContext[Job]?.getCancellationException()
                logger.debug("Job $jobState/$name was cancelled with cause: $cancellationCause", e)
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e,
                    e.message ?: "Asynchronous exception caught",
                    "coroutine name" to name,
                )
            }
        }

        if (timeout != Duration.INFINITE && timeout != Duration.ZERO) {
            launch {
                delay(timeout)
                if (mainJob.isActive) {
                    ErrorManager.logErrorStateWithData(
                        "Coroutine timed out",
                        "The coroutine '$name' took longer than the specified timeout of $timeout",
                        "timeout" to timeout,
                        "coroutine name" to name,
                    )
                    mainJob.cancel(CancellationException("Coroutine $name timed out after $timeout"))
                }
            }
        }
    }

    var screenToOpen: GuiScreen? = null
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
