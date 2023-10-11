package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod.Companion.consoleLog
import at.hannibal2.skyhanni.SkyHanniMod.Companion.coroutineScope
import at.hannibal2.skyhanni.SkyHanniMod.Companion.feature
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
import com.google.gson.JsonObject
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object DiscordRPCManager : IPCListener {
    private const val applicationID = 1093298182735282176L
    private const val updatePeriod = 4200L

    private val config get() = feature.misc.discordRPC

    private var client: IPCClient? = null
    private lateinit var secondLine: DiscordStatus
    private lateinit var firstLine: DiscordStatus
    private var startTimestamp: Long? = null
    private var startOnce = false

    private var updateTimer: Timer? = null
    private var connected = false

    private val DiscordLocationKey = DiscordLocationKey()

    fun start(fromCommand: Boolean = false) {
        coroutineScope.launch {
            try {
                if (isActive()) {
                    return@launch
                }
                consoleLog("Starting Discord RPC...")

                firstLine = getStatusByConfigId(config.firstLine.get())
                secondLine = getStatusByConfigId(config.secondLine.get())
                startTimestamp = System.currentTimeMillis()
                client = IPCClient(applicationID)
                client?.setListener(this@DiscordRPCManager)

                try {
                    client?.connect()
                    if (fromCommand) LorenzUtils.chat("§a[SkyHanni] Successfully started Rich Presence!") // confirm that /shrpcstart worked
                } catch (ex: Exception) {
                    consoleLog("Warn: Failed to connect to RPC!")
                    consoleLog(ex.toString())
                    LorenzUtils.clickableChat(
                        "§e[SkyHanni] Discord Rich Presence was unable to start! " +
                            "This usually happens when you join SkyBlock when Discord is not started. " +
                            "Please run /shrpcstart to retry once you have launched Discord.", "shrpcstart"
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
        onToggle(
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

    fun updatePresence() {
        val location = DiscordStatus.LOCATION.getDisplayString()
        val discordIconKey = DiscordLocationKey.getDiscordIconKey(location)

        secondLine = getStatusByConfigId(config.secondLine.get())
        firstLine = getStatusByConfigId(config.firstLine.get())
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
        updateTimer = Timer()
        updateTimer?.schedule(object : TimerTask() {
            override fun run() {
                updatePresence()
            }
        }, 0, updatePeriod)
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
        updateTimer?.let {
            it.cancel()
            updateTimer = null
        }
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
            LorenzUtils.chat("§c[SkyHanni] Discord Rich Presence is disabled. Enable it in the config §e/sh discord")
            return
        }

        if (isActive()) {
            LorenzUtils.chat("§e[SkyHanni] Discord Rich Presence is already active!")
            return
        }

        LorenzUtils.chat("§e[SkyHanni] Attempting to start Discord Rich Presence...")
        try {
            start(true)
        } catch (e: Exception) {
            LorenzUtils.chat("§c[SkyHanni] Unable to start Discord Rich Presence! Please report this on Discord and ping NetheriteMiner#6267.")
        }
    }
}
