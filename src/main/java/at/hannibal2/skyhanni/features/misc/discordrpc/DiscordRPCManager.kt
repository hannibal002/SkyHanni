package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod.Companion.consoleLog
import at.hannibal2.skyhanni.SkyHanniMod.Companion.coroutineScope
import at.hannibal2.skyhanni.SkyHanniMod.Companion.feature
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.LineEntry
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.jsonobjects.repo.StackingEnchantData
import at.hannibal2.skyhanni.data.jsonobjects.repo.StackingEnchantsJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.JsonObject
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.RichPresenceButton
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import kotlin.time.Duration.Companion.seconds

object DiscordRPCManager : IPCListener {

    private const val APPLICATION_ID = 1093298182735282176L

    val config get() = feature.gui.discordRPC

    private var client: IPCClient? = null
    private var startTimestamp: Long = 0
    private var started = false
    private var nextUpdate: SimpleTimeMark = SimpleTimeMark.farPast()

    var stackingEnchants: Map<String, StackingEnchantData> = emptyMap()

    fun start(fromCommand: Boolean = false) {
        coroutineScope.launch {
            try {
                if (isActive()) {
                    return@launch
                }
                consoleLog("Starting Discord RPC...")
                startTimestamp = System.currentTimeMillis()
                client = IPCClient(APPLICATION_ID)
                client?.setListener(this@DiscordRPCManager)

                try {
                    client?.connect()
                    if (fromCommand) ChatUtils.chat(
                        "Successfully started Rich Presence!",
                        prefixColor = "§a"
                    ) // confirm that /shrpcstart worked
                } catch (ex: Exception) {
                    consoleLog("Warn: Failed to connect to RPC!")
                    consoleLog(ex.toString())
                    ChatUtils.clickableChat(
                        "Discord Rich Presence was unable to start! " +
                            "This usually happens when you join SkyBlock when Discord is not started. " +
                            "Please run /shrpcstart to retry once you have launched Discord.",
                        onClick = {
                            startCommand()
                        }
                    )
                }
            } catch (ex: Throwable) {
                consoleLog("Warn: Discord RPC has thrown an unexpected error while trying to start...")
                consoleLog(ex.toString())
            }
        }
    }

    private fun stop() {
        coroutineScope.launch {
            if (isActive()) {
                client?.close()
                started = false
            }
        }
    }

    private fun isActive() = client?.status == PipeStatus.CONNECTED

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.firstLine, config.secondLine, config.customText) {
            if (isActive()) {
                updatePresence()
            }
        }
        config.enabled.whenChanged { _, new ->
            if (!new) {
                stop()
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        stackingEnchants = event.getConstant<StackingEnchantsJson>("StackingEnchants").enchants
    }

    private fun updatePresence() {
        val location = DiscordStatus.LOCATION.getDisplayString()
        val discordIconKey = DiscordLocationKey.getDiscordIconKey(location)
        client?.sendRichPresence(RichPresence.Builder().apply {
            setDetails(getStatusByConfigId(config.firstLine.get()).getDisplayString())
            setState(getStatusByConfigId(config.secondLine.get()).getDisplayString())
            setStartTimestamp(startTimestamp)
            setLargeImage(discordIconKey, location)

            if (config.showSkyCryptButton.get()) {
                addButton(RichPresenceButton(
                    "https://sky.shiiyu.moe/stats/${LorenzUtils.getPlayerName()}/${HypixelData.profileName}",
                    "Open SkyCrypt Profile"
                ))
            }
        }.build())
    }

    override fun onReady(client: IPCClient) {
        consoleLog("Discord RPC Started.")
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isActive()) return
        if (event.repeatSeconds(5)) {
            updatePresence()
        }
    }

    override fun onClose(client: IPCClient, json: JsonObject?) {
        consoleLog("Discord RPC closed.")
        this.client = null
    }

    override fun onDisconnect(client: IPCClient?, t: Throwable?) {
        consoleLog("Discord RPC disconnected.")
        this.client = null
    }

    private fun getStatusByConfigId(entry: LineEntry) =
        DiscordStatus.entries.getOrElse(entry.ordinal) { DiscordStatus.NONE }

    private fun isEnabled() = config.enabled.get()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        // the mod has already started the connection process. this variable is my way of running a function when
        // the player joins SkyBlock but only running it again once they join and leave.
        if (started || !isEnabled()) return
        if (LorenzUtils.inSkyBlock) {
            start()
            started = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (nextUpdate.isInFuture()) return
        // wait 5 seconds to check if the new world is skyblock or not before stopping the function
        nextUpdate = DelayedRun.runDelayed(5.seconds) {
            if (!LorenzUtils.inSkyBlock) {
                stop()
            }
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        stop()
    }

    fun startCommand() {
        if (!config.enabled.get()) {
            ChatUtils.userError("Discord Rich Presence is disabled. Enable it in the config §e/sh discord")
            return
        }

        if (isActive()) {
            ChatUtils.userError("Discord Rich Presence is already active!")
            return
        }

        ChatUtils.chat("Attempting to start Discord Rich Presence...")
        try {
            start(true)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Unable to start Discord Rich Presence! Please report this on Discord and ping @netheriteminer."
            )
        }
    }

    // Events that change things in DiscordStatus
    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!isEnabled() || !PriorityEntry.AFK.isSelected()) return // autoPriority 4 is dynamic afk
        beenAfkFor = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(11, "misc.discordRPC.firstLine") { element ->
            ConfigUtils.migrateIntToEnum(element, LineEntry::class.java)
        }
        event.transform(11, "misc.discordRPC.secondLine") { element ->
            ConfigUtils.migrateIntToEnum(element, LineEntry::class.java)
        }
        event.transform(11, "misc.discordRPC.auto") { element ->
            ConfigUtils.migrateIntToEnum(element, LineEntry::class.java)
        }
        event.transform(11, "misc.discordRPC.autoPriority") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, PriorityEntry::class.java)
        }

        event.move(31, "misc.discordRPC", "gui.discordRPC")
    }

    private fun PriorityEntry.isSelected() = config.autoPriority.contains(this)
}
