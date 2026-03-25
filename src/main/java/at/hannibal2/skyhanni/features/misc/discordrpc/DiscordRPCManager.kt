package at.hannibal2.skyhanni.features.misc.discordrpc

// originally adapted from SkyblockAddons

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.feature
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.LineEntry
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConnectionRetryHelper
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.addSkyHanniUtm
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DiscordRPCManager {

    private const val APPLICATION_ID = 1093298182735282176L

    val config get() = feature.gui.discordRPC

    private var client: DiscordIPC? = null
    private var startTimestamp = SimpleTimeMark.farPast()
    private var started = false
    private var nextUpdate = SimpleTimeMark.farPast()
    private var presenceJob: Job? = null
    private var readerJob: Job? = null

    private var debugError = false
    private var debugStatusMessage = "nothing"

    private val progressCategory = ChatProgressUpdates.category("Discord RPC")
    private val retryHelper = ConnectionRetryHelper(listOf(10.seconds, 20.seconds, 30.seconds))
    private var retryJob: Job? = null
    private var lastDebugInfo: Map<String, String> = emptyMap()

    private val startConfig = CoroutineConfig("discord rpc start", timeout = Duration.INFINITE).withIOContext()
    private val presenceConfig = CoroutineConfig("discord rpc updatePresence", timeout = Duration.INFINITE).withIOContext()
    private val readerConfig = CoroutineConfig("discord rpc reader", timeout = Duration.INFINITE).withIOContext()
    private val stopConfig = CoroutineConfig("discord rpc stop", timeout = Duration.INFINITE).withIOContext()
    private val manualStartConfig = CoroutineConfig("discord rpc manual start", timeout = Duration.INFINITE).withIOContext()

    private fun start(progress: ChatProgressUpdates, fromCommand: Boolean = false) {
        progress.update("call start")
        if (isConnected()) {
            progress.end("alr connected")
            return
        }
        updateDebugStatus("Starting...")
        startTimestamp = SimpleTimeMark.now()
        try {
            DiscordIPC(APPLICATION_ID, onDebugInfo = { lastDebugInfo = it }).also {
                it.connect()
                client = it
            }
            setupPresenceJob(progress)
            retryJob?.cancel()
            retryHelper.reset()
            progress.end("Successfully started")
            updateDebugStatus("Successfully started")
            if (fromCommand) ChatUtils.chat("Successfully started Rich Presence!", prefixColor = "§a")
        } catch (e: DiscordIPCException) {
            progress.end("discord not detected: ${e.message}")
            if (e.isSandboxIssue) {
                updateDebugStatus(e.message ?: "sandbox issue", error = true)
                ChatUtils.userError(e.message ?: "Discord RPC is blocked by a sandbox restriction")
            } else {
                scheduleRetry(e.message)
            }
        } catch (e: Throwable) {
            progress.end("error: ${e.message}")
            updateDebugStatus("Unexpected error: ${e.message}", error = true)
            ErrorManager.logErrorWithData(e, "Discord RPC has thrown an unexpected error while trying to start")
        }
    }

    private fun scheduleRetry(reason: String? = null) {
        val retryDelay = retryHelper.onFailure()
        if (retryDelay != null) {
            updateDebugStatus("Retry ${retryHelper.retriesLabel} in ${retryDelay.inWholeSeconds}s: ${reason ?: "unknown"}")
            val retryCount = retryHelper.currentRetry
            retryJob = with(SkyHanniMod) {
                CoroutineConfig("discord rpc autoretry $retryCount", timeout = Duration.INFINITE).withIOContext()
                    .launchUnScopedCoroutine {
                        delay(retryDelay)
                        start(progressCategory.start("discord rpc autoretry $retryCount"))
                    }
            }
        } else {
            updateDebugStatus("Discord not detected after all retries")
            ChatUtils.clickableChat(
                message = "Discord RPC could not connect automatically. Click to retry!",
                onClick = ::startCommand,
                hover = "§eClick to run /shrpcstart!",
            )
        }
    }

    private fun stop() {
        if (!isConnected()) return
        updateDebugStatus("Stopped")
        readerJob?.cancel()
        readerJob = null
        client?.close()
        client = null
        started = false
    }

    private fun isConnected() = client?.isConnected == true

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.firstLine, config.secondLine, config.customText) {
            val progress = progressCategory.start("onToggle")
            if (isConnected()) {
                setupPresenceJob(progress)
                progress.end("Successfully updated")
            } else presenceJob?.cancel()
        }
        config.enabled.whenChanged { _, new ->
            if (!new) with(SkyHanniMod) { stopConfig.launchUnScopedCoroutine { stop() } }
        }
    }

    private fun setupPresenceJob(progress: ChatProgressUpdates) {
        presenceJob?.cancel()
        readerJob?.cancel()
        progress.update("in setupPresenceJob")
        var updatePresenceProgress: ChatProgressUpdates? = progressCategory.start("discord rpc updatePresence")
        presenceJob = with(SkyHanniMod) {
            presenceConfig.launchUnScopedCoroutine {
                updatePresenceProgress?.update("started update presence loop first run")
                while (isConnected()) {
                    updatePresence(updatePresenceProgress)
                    updatePresenceProgress?.end("update presence loop finished first run, not logging further updates")
                    updatePresenceProgress = null
                    delay(5.seconds)
                }
            }
        }
        val activeClient = client
        readerJob = with(SkyHanniMod) {
            readerConfig.launchUnScopedCoroutine {
                activeClient?.readerLoop()
            }
        }
    }

    private fun getSkyCryptUrl() =
        "https://sky.shiiyu.moe/stats/${PlayerUtils.getName()}/${HypixelData.profileName.firstLetterUppercase()}".addSkyHanniUtm()

    private fun getEliteSbUrl() =
        "${EliteDevApi.ELITE_URL}/@${PlayerUtils.getName()}/${HypixelData.profileName}".addSkyHanniUtm()

    private fun updatePresence(progress: ChatProgressUpdates?) {
        progress?.update("start in updatePresence")
        val location = DiscordStatus.LOCATION.getDisplayString()
        val details = getStatusByConfigId(config.firstLine.get()).getDisplayString()
        val state = getStatusByConfigId(config.secondLine.get()).getDisplayString()

        progress?.update("firstLine: ${config.firstLine.get()}, secondLine: ${config.secondLine.get()}")
        progress?.update("details: $details, state: $state")

        val presence = DiscordRichPresence(
            details = details,
            state = state,
            startTimestamp = startTimestamp.toMillis() / 1000L,
            largeImageKey = DiscordLocationKey.getDiscordIconKey(location),
            largeImageText = location,
            buttons = buildList {
                if (config.showEliteSkyBlockButton.get()) DiscordRichPresence.Button(
                    label = "Open EliteSkyBlock",
                    url = getEliteSbUrl(),
                ).let { add(it) }
                if (config.showSkyCryptButton.get()) DiscordRichPresence.Button(
                    label = "Open SkyCrypt",
                    url = getSkyCryptUrl(),
                ).let { add(it) }
            },
        )

        try {
            client?.setActivity(presence)
        } catch (e: DiscordIPCException) {
            updateDebugStatus("Discord RPC disconnected: ${e.message}")
            client = null
            scheduleRetry("Discord RPC disconnected")
        }
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!isConnected()) return presenceJob?.cancel() ?: Unit
        if (presenceJob?.isActive == true) return
        val progress = progressCategory.start("onSecondPassed")
        setupPresenceJob(progress)
        progress.end("Successfully updated")
    }

    @HandleEvent
    fun onClientShutdown() {
        stop()
    }

    private fun getStatusByConfigId(entry: LineEntry) =
        DiscordStatus.entries.getOrElse(entry.ordinal) { DiscordStatus.NONE }

    private fun isEnabled() = config.enabled.get()

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (started || !isEnabled()) return
        val progress = progressCategory.start("auto start in onTick")
        with(SkyHanniMod) { startConfig.launchUnScopedCoroutine { start(progress) } }
        started = true
    }

    @HandleEvent
    fun onWorldChange() {
        if (nextUpdate.isInFuture()) return
        nextUpdate = DelayedRun.runDelayed(5.seconds) {
            if (!SkyBlockUtils.inSkyBlock) stop()
        }
    }

    @HandleEvent(ClientDisconnectEvent::class)
    fun onDisconnect() {
        retryJob?.cancel()
        retryHelper.reset()
        stop()
    }

    private fun startCommand() {
        val progress = progressCategory.start("init /shrpcstart")
        if (!isEnabled()) {
            progress.end("disabled in config")
            ChatUtils.userError("Discord Rich Presence is disabled. Enable it in the config §e/sh discord")
            return
        }
        if (isConnected()) {
            progress.end("already connected")
            ChatUtils.userError("Discord Rich Presence is already active!")
            return
        }
        retryJob?.cancel()
        retryHelper.reset()
        ChatUtils.chat("Attempting to start Discord Rich Presence...")
        progress.end("launchCoroutine")
        with(SkyHanniMod) {
            manualStartConfig.launchUnScopedCoroutine {
                start(progressCategory.start("discord rpc manual start"), fromCommand = true)
            }
        }
    }

    private fun updateDebugStatus(message: String, error: Boolean = false) {
        debugStatusMessage = message
        debugError = error
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Discord RPC")
        if (debugError) event.addData {
            add("Error detected!")
            add(debugStatusMessage)
            lastDebugInfo.forEach { (k, v) -> add("$k: $v") }
        } else event.addIrrelevant {
            add("no error detected.")
            add("status: $debugStatusMessage")
            add("lastActivityJson: ${client?.lastActivityJson ?: "none yet"}")
            lastDebugInfo.forEach { (k, v) -> add("$k: $v") }
        }
    }

    @HandleEvent(KeyPressEvent::class)
    fun onKeyPress() {
        if (!isEnabled() || !PriorityEntry.AFK.isSelected()) return
        beenAfkFor = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(31, "misc.discordRPC", "gui.discordRPC")
    }

    private fun PriorityEntry.isSelected() = config.autoPriority.contains(this)

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shrpcstart") {
            description = "Manually starts the Discord Rich Presence feature"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { startCommand() }
        }
    }
}
