package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import com.google.gson.JsonObject
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import kotlinx.coroutines.launch
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class DiscordRPCManager: IPCListener {

    private val applicationID: Long = 1093298182735282176L
    private val updatePeriod: Long = 4200L

    private val config get() = feature.misc

    private var client: IPCClient? = null
    private lateinit var detailsLine: DiscordStatus
    private lateinit var stateLine: DiscordStatus
    private var startTimestamp: Long? = null

    private var updateTimer: Timer? = null
    private var connected: Boolean = false

    private fun start() {
        coroutineScope.launch {
            try {
                if (isActive()) {
                    return@launch
                }
                consoleLog("Starting Discord RPC...")

                stateLine = getStatusByConfigId(config.status)
                detailsLine = getStatusByConfigId(config.details)
                startTimestamp = System.currentTimeMillis()
                client = IPCClient(applicationID)
                client?.setListener(this@DiscordRPCManager) // why must kotlin be this way

                try {
                    client?.connect()
                }
                catch(ex: Exception) {
                    consoleLog("Warn: Failed to connect to RPC!")
                    consoleLog(ex.toString())
                }
            }
            catch (ex: Throwable) {
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
            }
        }
    }

    private fun isActive(): Boolean {
        return client !=  null && connected
    }

    fun updatePresence() {
//        val discordIconKey: String = LocationRPC.getDiscordIconKey(location)

        stateLine = getStatusByConfigId(config.status)
        detailsLine = getStatusByConfigId(config.details)
        val largeImageDesc: String = LorenzUtils.skyBlockArea
        val smallImageDesc = "Using SkyHanni v$VERSION by Hannibal2"
        val presence: RichPresence = RichPresence.Builder()
            .setState(stateLine.getDisplayString(DiscordStatusEntry.STATE))
            .setDetails(detailsLine.getDisplayString(DiscordStatusEntry.DETAILS))
            .setStartTimestamp(startTimestamp!!)
            .setLargeImage("skyblock-logo", largeImageDesc)
            .setSmallImage("skyhanni-logo", smallImageDesc)
            .build()
        try {
            client?.sendRichPresence(presence)
        }
        catch(e: Exception) {
            if (isEnabled() && LorenzUtils.inSkyBlock) {
                start()
            }
        }
    }

//    fun setStateLine(status: DiscordStatus) {
//        this.stateLine = status
//        if (isActive()) {
//            updatePresence()
//        }
//    }
//
//    fun setDetailsLine(status: DiscordStatus) {
//        this.detailsLine = status
//        if (isActive()) {
//            updatePresence()
//        }
//    }

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
        if (updateTimer !=  null) {
            updateTimer?.cancel()
            updateTimer = null
        }
    }

    private fun getStatusByConfigId(id: Int): DiscordStatus { // there's probably a better way to do this than hardcoding it but I don't know how. you have to update the config to include the same text in the same order when you update it.
        return when(id) {
            0 ->  DiscordStatus.NONE
            1 ->  DiscordStatus.LOCATION
            2 ->  DiscordStatus.PURSE
            3 ->  DiscordStatus.BITS
            4 ->  DiscordStatus.ITEM
            5 ->  DiscordStatus.TIME
            6 ->  DiscordStatus.PROFILE
            7 ->  DiscordStatus.CUSTOM
            else ->  DiscordStatus.NONE
        }
    }

    companion object {
        lateinit var currentEntry: DiscordStatusEntry
    }

    private fun isEnabled(): Boolean {
        return config.discordRPCEnabled
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        executor.schedule({
            if (LorenzUtils.inSkyBlock && isEnabled()) {
                cancelTimer()
                start()
            }
            else {
                stop()
            }
        }, 5, TimeUnit.SECONDS) // wait 5 seconds to check if the new world is skyblock or not before stopping the function
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        stop()
    }
}