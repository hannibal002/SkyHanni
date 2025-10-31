package at.hannibal2.hanni

import at.hannibal2.hanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.event.HanniEvents
import at.hannibal2.hanni.config.ConfigFileType
import at.hannibal2.hanni.config.ConfigGuiManager.openConfigGui
import at.hannibal2.hanni.config.ConfigManager
import at.hannibal2.hanni.config.Features
import at.hannibal2.hanni.config.SackData
import at.hannibal2.hanni.config.StorageData
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.config.storage.OrderedWaypointsRoutes
import at.hannibal2.hanni.data.GuiEditManager
import at.hannibal2.hanni.data.OtherInventoryData
import at.hannibal2.hanni.data.PetDataStorage
import at.hannibal2.hanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.hanni.data.jsonobjects.local.JacobContestsJson
import at.hannibal2.hanni.data.jsonobjects.local.KnownFeaturesJson
import at.hannibal2.hanni.data.jsonobjects.local.VisualWordsJson
import at.hannibal2.hanni.data.repo.HanniRepoManager
import at.hannibal2.hanni.events.utils.InitFinishedEvent
import at.hannibal2.hanni.events.utils.PreInitFinishedEvent
import at.hannibal2.hanni.hannimodule.LoadedModules
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.MinecraftConsoleFilter
import at.hannibal2.hanni.utils.VersionConstants
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.system.ModVersion
import at.hannibal2.hanni.utils.system.PlatformUtils
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HanniModule
object HanniMod {

    fun preInit() {
        PlatformUtils.checkIfNeuIsLoaded()

        LoadedModules.modules.forEach { HanniModLoader.loadModule(it) }

        HanniEvents.init(modules)

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
            HanniRepoManager.initRepo()
        } catch (e: Exception) {
            Exception("Error reading repo data", e).printStackTrace()
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

    const val MODID: String = "hanni"
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
    val logger: Logger = LogManager.getLogger("Hanni")
    fun getLogger(name: String): Logger {
        return LogManager.getLogger("Hanni.$name")
    }

    val modules: MutableList<Any> = ArrayList()
    private val globalJob: Job = Job(null)
    private val coroutineScope = CoroutineScope(
        CoroutineName("Hanni") + SupervisorJob(globalJob),
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
     * Launch an IO coroutine in the Hanni scope.
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
     * Launches a coroutine in the Hanni scope.
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
     * Launches a coroutine in the Hanni scope.
     * This coroutine will catch any exceptions thrown by the provided function.
     * @param function The suspend function to execute in the coroutine.
     */
    @OptIn(InternalCoroutinesApi::class)
    fun launchCoroutine(
        name: String,
        timeout: Duration = 10.seconds,
        function: suspend CoroutineScope.() -> Unit,
    ): Job = coroutineScope.launch(CoroutineName("Hanni $name")) {
        try {
            if (timeout != Duration.INFINITE && timeout > Duration.ZERO) {
                withTimeout(timeout) { function() }
            } else {
                function()
            }
        } catch (e: TimeoutCancellationException) {
            ErrorManager.logErrorWithData(
                e,
                "Coroutine $name timed out after $timeout",
                "coroutine name" to name,
                "timeout" to timeout,
            )
            throw e
        } catch (e: CancellationException) {
            // Don't notify the user about cancellation exceptions - these are to be expected at times
            val jobState = coroutineContext[Job]?.toString() ?: "unknown job"
            val cancellationCause = coroutineContext[Job]?.getCancellationException()
            logger.debug("Job $jobState/$name was cancelled with cause: $cancellationCause", e)
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(
                e,
                "Asynchronous exception caught in $name",
                "coroutine name" to name,
                "coroutine timeout" to timeout,
            )
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
            aliases = listOf("hanni")
            description = "Opens the main Hanni config"
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
