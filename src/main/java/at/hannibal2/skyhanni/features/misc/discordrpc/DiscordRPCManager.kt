package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod.Companion.consoleLog
import at.hannibal2.skyhanni.SkyHanniMod.Companion.coroutineScope
import at.hannibal2.skyhanni.SkyHanniMod.Companion.feature
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.LineEntry
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig.PriorityEntry
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
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.JsonObject
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object DiscordRPCManager : IPCListener {

    private const val applicationID = 1093298182735282176L

    val config get() = feature.gui.discordRPC

    private var client: IPCClient? = null
    private lateinit var secondLine: DiscordStatus
    private lateinit var firstLine: DiscordStatus
    private var startTimestamp: Long? = null
    private var startOnce = false

    private var runTimer = false
    private var connected = false

    private val DiscordLocationKey = DiscordLocationKey()

    var stackingEnchants: Map<String, StackingEnchantData> = emptyMap()

    fun start(fromCommand: Boolean = false) {
        coroutineScope.launch {
            try {
                if (isActive()) {
                    return@launch
                }
                consoleLog("Starting Discord RPC...")
                // TODO, change functionality to use enum rather than ordinals
                firstLine = getStatusByConfigId(config.firstLine.get().ordinal)
                secondLine = getStatusByConfigId(config.secondLine.get().ordinal)
                startTimestamp = System.currentTimeMillis()
                client = IPCClient(applicationID)
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
                connected = false
                client?.close()
                startOnce = false
            }
        }
    }

    private fun isActive() = client != null && connected

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.firstLine,
            config.secondLine,
            config.customText
        ) {
            if (isActive()) {
                updatePresence()
            }
        }
        config.enabled.whenChanged { _, new ->
            if (new) {
//                start()
            } else {
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
        // TODO, change functionality to use enum rather than ordinals
        secondLine = getStatusByConfigId(config.secondLine.get().ordinal)
        firstLine = getStatusByConfigId(config.firstLine.get().ordinal)
        val presence: RichPresence = RichPresence.Builder()
            .setDetails(firstLine.getDisplayString())
            .setState(secondLine.getDisplayString())
            .setStartTimestamp(startTimestamp!!)
            .setLargeImage(discordIconKey, location)
            .build()
        client?.sendRichPresence(presence)
    }

    override fun onReady(client: IPCClient) {
        consoleLog("Discord RPC Started.")
        connected = true
        runTimer = true
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!runTimer) return
        if (event.repeatSeconds(5)) {
            updatePresence()
        }
    }

    override fun onClose(client: IPCClient, json: JsonObject?) {
        consoleLog("Discord RPC closed.")
        this.client = null
        connected = false
        cancelTimer()
    }

    override fun onDisconnect(client: IPCClient?, t: Throwable?) {
        consoleLog("Discord RPC disconnected.")
        this.client = null
        connected = false
        cancelTimer()
    }

    private fun cancelTimer() {
        runTimer = false
    }

    private fun getStatusByConfigId(id: Int) = DiscordStatus.entries.getOrElse(id) { DiscordStatus.NONE }

    private fun isEnabled() = config.enabled.get()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (startOnce || !isEnabled()) return // the mod has already started the connection process. this variable is my way of running a function when the player joins SkyBlock but only running it again once they join and leave.
        if (LorenzUtils.inSkyBlock) {
            start()
            startOnce = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        executor.schedule(
            {
                if (!LorenzUtils.inSkyBlock) {
                    stop()
                }
            },
            5,
            TimeUnit.SECONDS
        ) // wait 5 seconds to check if the new world is skyblock or not before stopping the function
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
