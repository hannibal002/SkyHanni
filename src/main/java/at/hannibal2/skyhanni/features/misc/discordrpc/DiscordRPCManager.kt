package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.feature
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
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.data.ErrorEventData
import dev.cbyrne.kdiscordipc.core.event.impl.DisconnectedEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ErrorEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.Activity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DiscordRPCManager {

    private const val APPLICATION_ID = 1093298182735282176L

    val config get() = feature.gui.discordRPC

    private var client: KDiscordIPC? = null
    private var startTimestamp: SimpleTimeMark = SimpleTimeMark.farPast()
    private var started = false
    private var nextUpdate: SimpleTimeMark = SimpleTimeMark.farPast()
    private var presenceJob: Job? = null

    private var debugError = false
    private var debugStatusMessage = "nothing"

    private val progressCategory = ChatProgressUpdates.category("Discord RPC")

    suspend fun start(progress: ChatProgressUpdates, fromCommand: Boolean = false) {
        progress.update("call start")
        if (isConnected()) {
            progress.end("alr connected")
            return
        }
        progress.update("Starting...")
        updateDebugStatus("Starting...")
        startTimestamp = SimpleTimeMark.now()
        progress.update("calling KDiscordIPC")
        client = KDiscordIPC(APPLICATION_ID.toString())
        progress.update("done init client")
        try {
            progress.update("calling setup")
            setup(progress, fromCommand)
            progress.end("setup done successfully")
        } catch (e: Throwable) {
            progress.end("error: ${e.message}")
            updateDebugStatus("Unexpected error: ${e.message}", error = true)
            ErrorManager.logErrorWithData(e, "Discord RPC has thrown an unexpected error while trying to start")
        }
    }

    private fun stop() {
        if (!isConnected()) return
        updateDebugStatus("Stopped")
        client?.disconnect()
        started = false
    }

    private suspend fun setup(progress: ChatProgressUpdates, fromCommand: Boolean) {
        try {
            progress.update("on<ReadyEvent>")
            client?.on<ReadyEvent> { onReady() }
            progress.update("on<DisconnectedEvent>")
            client?.on<DisconnectedEvent> { onIPCDisconnect() }
            progress.update("on<ErrorEvent>")
            client?.on<ErrorEvent> { onError(data) }
            progress.update("connect")
            client?.connect()
            progress.update("call setupPresenceJob")
            setupPresenceJob(progress)
            progress.end("Successfully started")
            updateDebugStatus("Successfully started")
            if (!fromCommand) return
            // confirm that /shrpcstart worked
            ChatUtils.chat("Successfully started Rich Presence!", prefixColor = "§a")
        } catch (e: Exception) {
            updateDebugStatus("Failed to connect: ${e.message}", error = true)
            ErrorManager.logErrorWithData(
                e,
                "Discord Rich Presence was unable to start! " +
                    "This was probably NOT due to something you did. " +
                    "Please report this and ping NetheriteMiner.",
            )
            ChatUtils.clickableChat(
                "Click here to retry.",
                onClick = ::startCommand,
                "§eClick to run /shrpcstart!",
            )
        }
    }

    private fun isConnected() = client?.connected == true

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
            if (!new) stop()
        }
    }

    private fun setupPresenceJob(progress: ChatProgressUpdates) {
        progress.update("in setupPresenceJob")
        presenceJob = SkyHanniMod.launchNoScopeCoroutine("discord rpc updatePresence", timeout = Duration.INFINITE) {
            progress.update("started update presence loop")
            var temp: ChatProgressUpdates? = progress
            while (isConnected()) {
                updatePresence(temp)
                temp = null
                delay(5.seconds)
            }
        }
    }

    private suspend fun updatePresence(progress: ChatProgressUpdates?) {
        progress?.update("start in updatePresence")
        val location = DiscordStatus.LOCATION.getDisplayString()
        val discordIconKey = DiscordLocationKey.getDiscordIconKey(location)
        val buttons = mutableListOf<Activity.Button>()
        progress?.update("start creating buttons")
        if (config.showEliteBotButton.get()) {
            buttons.add(
                Activity.Button(
                    label = "Open EliteBot",
                    url = "https://elitebot.dev/@${PlayerUtils.getName()}/${HypixelData.profileName}",
                ),
            )
        }

        if (config.showSkyCryptButton.get()) {
            buttons.add(
                Activity.Button(
                    label = "Open SkyCrypt",
                    url = "https://sky.shiiyu.moe/stats/${PlayerUtils.getName()}/${HypixelData.profileName}",
                ),
            )
        }

        progress?.update("start creating activity")
        val entry = config.secondLine.get()
        val statusByConfigId = getStatusByConfigId(entry)
        val state = statusByConfigId.getDisplayString()
        progress?.update("firstLine: ${config.firstLine.get()}")
        progress?.update("secondLine: ${config.secondLine.get()}")
        val details = getStatusByConfigId(config.firstLine.get()).getDisplayString()
        progress?.update("details: $details")
        progress?.update("state: $state")
        client?.activityManager?.setActivity(
            Activity(
                details = details,
                state = state,
                timestamps = Activity.Timestamps(
                    start = startTimestamp.toMillis(),
                    end = null,
                ),
                assets = Activity.Assets(
                    largeImage = discordIconKey,
                    largeText = location,
                ),
                buttons = buttons.ifEmpty { null },
            ),
        )
    }


    private fun onReady() {
        updateDebugStatus("Discord RPC Ready.")
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!isConnected()) return presenceJob?.cancel() ?: Unit
        else if (presenceJob?.isActive == true) return
        val progress = progressCategory.start("onSecondPassed")
        setupPresenceJob(progress)
        progress.end("Successfully updated")
    }

    private fun onIPCDisconnect() {
        updateDebugStatus("Discord RPC disconnected.")
        this.client = null
    }

    private fun onError(data: ErrorEventData) {
        updateDebugStatus("Discord RPC Errored. Error code ${data.code}: ${data.message}", true)
    }

    private fun getStatusByConfigId(entry: LineEntry): DiscordStatus {
        return DiscordStatus.entries.getOrElse(entry.ordinal) { DiscordStatus.NONE }
    }

    private fun isEnabled() = config.enabled.get()

    @HandleEvent
    fun onTick() {
        // The mod has already started the connection process. This variable is my way of running a function when
        // the player joins SkyBlock but only running it again once they join and leave.
        if (started || !isEnabled()) return
        if (SkyBlockUtils.inSkyBlock) {
            val progress = progressCategory.start("auto start in onTick")
            SkyHanniMod.launchNoScopeCoroutine("discord rpc start", timeout = INFINITE) { start(progress) }
            started = true
        }
    }

    @HandleEvent
    fun onWorldChange() {
        if (nextUpdate.isInFuture()) return
        // wait 5 seconds to check if the new world is skyblock or not before stopping the function
        nextUpdate = DelayedRun.runDelayed(5.seconds) {
            if (!SkyBlockUtils.inSkyBlock) stop()
        }
    }

    @HandleEvent(ClientDisconnectEvent::class)
    fun onDisconnect() {
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

        progress.end("attempting to start")
        ChatUtils.chat("Attempting to start Discord Rich Presence...")
        try {
            progress.end("launchCoroutine")
            SkyHanniMod.launchCoroutine("discord rpc manual start") { start(progress, true) }
            updateDebugStatus("Successfully started")
        } catch (e: Exception) {
            updateDebugStatus("Unable to start: ${e.message}", error = true)
            ErrorManager.logErrorWithData(
                e,
                "Unable to start Discord Rich Presence! Please report this on Discord and ping @netheriteminer.",
            )
        }
    }

    private fun updateDebugStatus(message: String, error: Boolean = false) {
        debugStatusMessage = message
        debugError = error
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Discord RPC")

        if (debugError) {
            event.addData {
                add("Error detected!")
                add(debugStatusMessage)
            }
        } else {
            event.addIrrelevant {
                add("no error detected.")
                add("status: $debugStatusMessage")
            }
        }
    }

    // Events that change things in DiscordStatus
    @HandleEvent(KeyPressEvent::class)
    fun onKeyPress() {
        if (!isEnabled() || !PriorityEntry.AFK.isSelected()) return // autoPriority 4 is dynamic afk
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
