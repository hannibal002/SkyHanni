package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod.Companion.consoleLog
import at.hannibal2.skyhanni.SkyHanniMod.Companion.coroutineScope
import at.hannibal2.skyhanni.SkyHanniMod.Companion.feature
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
import com.google.gson.JsonObject
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import kotlinx.coroutines.launch
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class DiscordRPCManager : IPCListener {
    private val applicationID = 653443797182578707L
    private val updatePeriod = 4200L

    private val config get() = feature.misc.discordRPC

    private var client: IPCClient? = null
    private lateinit var secondLine: DiscordStatus
    private lateinit var firstLine: DiscordStatus
    private var startTimestamp: Long? = null
    private var startOnce = false

    private var updateTimer: Timer? = null
    private var connected = false

    private val DiscordLocationKey = DiscordLocationKey()

    private fun start() {
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
                } catch (ex: Exception) {
                    consoleLog("Warn: Failed to connect to RPC!")
                    consoleLog(ex.toString())
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
        onToggle(config.firstLine,
            config.secondLine,
            config.customText) {
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
        val location = LorenzUtils.skyBlockArea
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

    override fun onClose(client: IPCClient, json: JsonObject) {
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

    private fun getStatusByConfigId(id: Int) = DiscordStatus.values().getOrElse(id) { DiscordStatus.NONE }

    private fun isEnabled() = config.enabled.get()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (startOnce || !isEnabled()) return // the mod has already started the connection process. this variable is my way of running a function when the player joins skyblock but only running it again once they join and leave.
        if (LorenzUtils.inSkyBlock) {
            start()
            startOnce = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
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
}
