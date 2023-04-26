package at.hannibal2.skyhanni.features.misc.discordrpc

// This entire file was taken from SkyblockAddons code, ported to SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod.*
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import com.google.gson.JsonObject
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import io.github.moulberry.moulconfig.observer.Property
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

    private val applicationID: Long = 653443797182578707L
    private val updatePeriod: Long = 4200L

    private val config get() = feature.misc.discordRPC

    private var client: IPCClient? = null
    private lateinit var detailsLine: DiscordStatus
    private lateinit var stateLine: DiscordStatus
    private var startTimestamp: Long? = null
    private var startOnce = false

    private var updateTimer: Timer? = null
    private var connected: Boolean = false

    private fun start() {
        coroutineScope.launch {
            try {
                if (isActive()) {
                    return@launch
                }
                consoleLog("Starting Discord RPC...")

                stateLine = getStatusByConfigId(config.status.get())
                detailsLine = getStatusByConfigId(config.details.get())
                startTimestamp = System.currentTimeMillis()
                client = IPCClient(applicationID)
                client?.setListener(this@DiscordRPCManager) // why must kotlin be this way

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

    private fun isActive(): Boolean {
        return client != null && connected
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for (property in listOf(
            config.enabled,
            config.details,
            config.status,
            config.customText,
        )) {
            property.whenChangedWithDifference { updatePresence() }
        }
    }

    fun Property<*>.whenChangedWithDifference(run: () -> (Unit)) {
        whenChanged { old, new -> if (old != new) run() }
    }

    fun updatePresence() {
        println("updatePresence")
        val location = LorenzUtils.skyBlockArea
        val discordIconKey = getDiscordIconKey(location)

        stateLine = getStatusByConfigId(config.status.get())
        detailsLine = getStatusByConfigId(config.details.get())
        val presence: RichPresence = RichPresence.Builder()
            .setState(stateLine.getDisplayString(DiscordStatusEntry.STATE))
            .setDetails(detailsLine.getDisplayString(DiscordStatusEntry.DETAILS))
            .setStartTimestamp(startTimestamp!!)
            .setLargeImage(discordIconKey, location)
            .build()
        client?.sendRichPresence(presence)
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

    private fun getDiscordIconKey(location: String): String {
        val normalRPC: Set<String> = setOf(
            "auction-house",
            "bank",
            "canvas-room",
            "coal-mine",
            "colosseum",
            "farm",
            "fashion-shop",
            "flower-house",
            "forest",
            "graveyard",
            "library",
            "mountain",
            "ruins",
            "tavern",
            "village",
            "wilderness",
            "wizard-tower",
            "birch-park",
            "spruce-woods",
            "savanna-woodland",
            "dark-thicket",
            "jungle-island",
            "gold-mine",
            "slimehill",
            "diamond-reserve",
            "obsidian-sanctuary",
            "the-barn",
            "mushroom-desert",
            "the-end"
        )
        // list of tokens where the name can just be lowercased and spaces can be replaced with dashes

        val specialRPC: Map<String, String> = mapOf(
            "Fisherman's Hut" to "fishermans-hut", "Unincorporated" to "high-level",
            "Dragon's Nest" to "dragons-nest", "Void Sepulture" to "the-end", "Void Slate" to "the-end",
            "Zealot Bruiser Hideout" to "the-end", "Desert Settlement" to "mushroom-desert",
            "Oasis" to "mushroom-desert", "Desert Mountain" to "mushroom-desert", "Jake's House" to "mushroom-desert",
            "Trapper's Den" to "mushroom-desert", "Mushroom Gorge" to "mushroom-desert",
            "Glowing Mushroom Cave" to "mushroom-desert", "Overgrown Mushroom Cave" to "mushroom-desert",
            "Shepherd's Keep" to "mushroom-desert", "Treasure Hunter Camp" to "mushroom-desert",
            "Windmill" to "the-barn", "Spider's Den" to "spiders-den", "Arachne's Burrow" to "spiders-den",
            "Arachne's Sanctuary" to "spiders-den", "Archaeologist's Camp" to "spiders-den",
            "Grandma's House" to "spiders-den", "Gravel Mines" to "spiders-den", "Spider Mound" to "spiders-den",
            "Melody's Plateau" to "forest", "Viking Longhouse" to "forest", "Lonely Island" to "forest",
            "Howling Cave" to "forest"
        ) // maps locations that do have a token, but have parentheses or a legacy key

        val specialNetherRPC: Array<String> = arrayOf(
            "Aura's Lab",
            "Barbarian Outpost",
            "Belly of the Beast",
            "Blazing Volcano",
            "Burning Desert",
            "Cathedral",
            "Chief's Hut",
            "Courtyard",
            "Crimson Fields",
            "Crimson Isle",
            "Dojo",
            "Dragontail Auction House",
            "Dragontail Bank",
            "Dragontail Bazaar",
            "Dragontail Blacksmith",
            "Dragontail Townsquare",
            "Dragontail",
            "Forgotten Skull",
            "Igrupan's Chicken Coop",
            "Igrupan's House",
            "Mage Council",
            "Mage Outpost",
            "Magma Chamber",
            "Matriarch's Lair",
            "Minion Shop",
            "Mystic Marsh",
            "Odger's Hut",
            "Plhlegblast Pool",
            "Ruins of Ashfang",
            "Scarleton Auction House",
            "Scarleton Bank",
            "Scarleton Bazaar",
            "Scarleton Blacksmith",
            "Scarleton Minion Shop",
            "Scarleton Plaza",
            "Scarleton",
            "Smoldering Tomb",
            "Stronghold",
            "The Bastion",
            "The Dukedom",
            "The Wasteland",
            "Throne Room"
        )
        // list of nether locations because there are sooo many (truncated some according to scoreboard)

        val keyIfNormal: String = location.lowercase().replace(' ', '-')

        return if (normalRPC.contains(keyIfNormal)) {
            keyIfNormal
        } else if (specialRPC.containsKey(location)) {
            specialRPC[location]!!
        } else if (specialNetherRPC.contains(location)) {
            "blazing-fortress"
        } else {
            "skyblock" // future proofing since we can't update the images anymore :(
        }
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
        if (updateTimer != null) {
            updateTimer?.cancel()
            updateTimer = null
        }
    }

    private fun getStatusByConfigId(id: Int) = DiscordStatus.values().getOrElse(id) { DiscordStatus.BITS }

    companion object {
        lateinit var currentEntry: DiscordStatusEntry
    }

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
