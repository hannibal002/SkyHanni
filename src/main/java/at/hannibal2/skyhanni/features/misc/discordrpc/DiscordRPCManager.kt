package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod.coroutineScope
import at.hannibal2.skyhanni.SkyHanniMod.feature
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.LineEntry
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.jsonobjects.repo.StackingEnchantData
import at.hannibal2.skyhanni.data.jsonobjects.repo.StackingEnchantsJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
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
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DiscordRPCManager {

    private const val APPLICATION_ID = 1093298182735282176L

    val config get() = feature.gui.discordRPC

    private var client: KDiscordIPC? = null
    private var startTimestamp: Long = 0
    private var started = false
    private var nextUpdate: SimpleTimeMark = SimpleTimeMark.farPast()

    var stackingEnchants: Map<String, StackingEnchantData> = emptyMap()

    private var debugError = false
    private var debugStatusMessage = "nothing"

    fun start(fromCommand: Boolean = false) {
        coroutineScope.launch {
            try {
                if (isConnected()) return@launch

                updateDebugStatus("Starting...")
                startTimestamp = System.currentTimeMillis()
                client = KDiscordIPC(APPLICATION_ID.toString())
                setup(fromCommand)
            } catch (e: Throwable) {
                updateDebugStatus("Unexpected error: ${e.message}", error = true)
                ErrorManager.logErrorWithData(e, "Discord RPC has thrown an unexpected error while trying to start")

            }
        }
    }

    private fun stop() {
        coroutineScope.launch {
            if (isConnected()) {
                updateDebugStatus("Stopped")
                client?.disconnect()
                started = false
            }
        }
    }

    private suspend fun setup(fromCommand: Boolean) {
        try {
            client?.on<ReadyEvent> { onReady() }
            client?.on<DisconnectedEvent> { onIPCDisconnect() }
            client?.on<ErrorEvent> { onError(data) }
            client?.connect()
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
                onClick = { startCommand() },
                "§eClick to run /shrpcstart!",
            )
        }
    }

    private fun isConnected() = client?.connected == true

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.firstLine, config.secondLine, config.customText) {
            if (isConnected()) {
                coroutineScope.launch {
                    updatePresence()
                }
            }
        }
        config.enabled.whenChanged { _, new ->
            if (!new) {
                stop()
            }
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        stackingEnchants = event.getConstant<StackingEnchantsJson>("StackingEnchants").enchants
    }

    private suspend fun updatePresence() {
        val location = DiscordStatus.LOCATION.getDisplayString()
        val discordIconKey = DiscordLocationKey.getDiscordIconKey(location)
        val buttons = mutableListOf<Activity.Button>()
        if (config.showEliteBotButton.get()) {
            buttons.add(
                Activity.Button(
                    label = "Open EliteBot",
                    url = "https://elitebot.dev/@${PlayerUtils.getName()}/${HypixelData.profileName}"
                )
            )
        }

        if (config.showSkyCryptButton.get()) {
            buttons.add(
                Activity.Button(
                    label = "Open SkyCrypt",
                    url = "https://sky.shiiyu.moe/stats/${PlayerUtils.getName()}/${HypixelData.profileName}"
                )
            )
        }

        client?.activityManager?.setActivity(
            Activity(
                details = getStatusByConfigId(config.firstLine.get()).getDisplayString(),
                state = getStatusByConfigId(config.secondLine.get()).getDisplayString(),
                timestamps = Activity.Timestamps(
                    start = startTimestamp,
                    end = null
                ),
                assets = Activity.Assets(
                    largeImage = discordIconKey,
                    largeText = location
                ),
                buttons = buttons.ifEmpty { null }
            )
        )
    }


    private fun onReady() {
        updateDebugStatus("Discord RPC Ready.")
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isConnected()) return
        if (event.repeatSeconds(5)) {
            coroutineScope.launch {
                updatePresence()
            }
        }
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
            // todo discord rpc doesnt connect on 1.21
            //#if TODO
            start()
            //#endif
            started = true
        }
    }

    @HandleEvent
    fun onWorldChange() {
        if (nextUpdate.isInFuture()) return
        // wait 5 seconds to check if the new world is skyblock or not before stopping the function
        nextUpdate = DelayedRun.runDelayed(5.seconds) {
            if (!SkyBlockUtils.inSkyBlock) {
                stop()
            }
        }
    }

    @HandleEvent(ClientDisconnectEvent::class)
    fun onDisconnect() {
        stop()
    }

    private fun startCommand() {
        if (!isEnabled()) {
            ChatUtils.userError("Discord Rich Presence is disabled. Enable it in the config §e/sh discord")
            return
        }

        if (isConnected()) {
            ChatUtils.userError("Discord Rich Presence is already active!")
            return
        }

        ChatUtils.chat("Attempting to start Discord Rich Presence...")
        try {
            start(true)
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
        event.register("shrpcstart") {
            description = "Manually starts the Discord Rich Presence feature"
            category = CommandCategory.USERS_ACTIVE
            callback { startCommand() }
        }
    }
}
