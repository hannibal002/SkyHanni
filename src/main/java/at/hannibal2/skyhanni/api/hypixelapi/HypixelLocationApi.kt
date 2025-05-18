package at.hannibal2.skyhanni.api.hypixelapi

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelLeaveEvent
import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiJoinEvent
import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiServerChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.ScoreboardTitleUpdateEvent
import at.hannibal2.skyhanni.events.skyblock.SkyBlockLeaveEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.annotations.Expose
import net.hypixel.data.type.GameType
import net.hypixel.data.type.LobbyType
import net.hypixel.data.type.ServerType

@Suppress("MemberVisibilityCanBePrivate")
@SkyHanniModule
object HypixelLocationApi {

    var inHypixel: Boolean = false
        private set

    var inSkyblock: Boolean = false
        private set

    var island: IslandType = IslandType.NONE
        private set

    var serverId: String? = null
        private set

    var inAlpha: Boolean = false
        private set

    var serverType: ServerType? = null
        private set

    var mode: String? = null
        private set

    var map: String? = null
        private set

    var isGuest: Boolean = false
        private set

    val inLimbo get() = serverId == "limbo"
    val inLobby get() = serverType == LobbyType.MAIN

    val config get() = SkyHanniMod.feature.dev.hypixelModApi

    private val logger = LorenzLogger("debug/hypixel_api")

    private var sentIslandEvent = false
    private var internalIsland = IslandType.NONE

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onHypixelJoin(event: HypixelApiJoinEvent) {
        logger.log(event.toString())
        logger.log("Connected to Hypixel")
        inAlpha = event.alpha
        inHypixel = true
        if (isModApiDetection) HypixelJoinEvent.post() // Temporary
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onServerChange(event: HypixelApiServerChangeEvent) {
        logger.log(event.toString())
        inHypixel = true
        val oldInSkyblock = inSkyblock
        inSkyblock = event.serverType == GameType.SKYBLOCK
        if (oldInSkyblock != inSkyblock) {
            if (!inSkyblock && isModApiDetection) SkyBlockLeaveEvent.post()
        }
        serverType = event.serverType
        mode = event.mode
        map = event.map
        serverId = event.serverName

        // Set island to NONE when you leave skyblock
        if (!inSkyblock) {
            internalIsland = IslandType.NONE
            changeIsland()
            return
        }
        val mode = event.mode ?: return

        val newIsland = IslandType.getByIdOrUnknown(mode)
        if (newIsland == IslandType.UNKNOWN) {
            ChatUtils.debug("Unknown island detected: '$newIsland'")
            logger.log("Unknown Island detected: '$newIsland'")
        } else {
            logger.log("Island detected: '$newIsland'")
        }
        internalIsland = newIsland

        // If the island has a guest variant, we wait for the scoreboard packet to confirm if it's a guest island or not
        if (internalIsland.hasGuestVariant()) {
            sentIslandEvent = false
        } else {
            sentIslandEvent = true
            changeIsland()
        }
    }

    @HandleEvent
    fun onScoreboardTitle(event: ScoreboardTitleUpdateEvent) {
        if (!inHypixel || !inSkyblock || sentIslandEvent || !event.isSkyblock) return
        isGuest = event.title.trim().removeColor().endsWith("GUEST")
        sentIslandEvent = true

        if (internalIsland.hasGuestVariant() && isGuest) {
            internalIsland = internalIsland.guestVariant()
        }

        changeIsland()
    }

    @HandleEvent
    fun onWorldChange() = resetWorldChange()

    private fun changeIsland() {
        if (internalIsland == island) return
        val oldIsland = island
        island = internalIsland
        logger.log("Island change: '$oldIsland' -> '$island'")
        if (!isModApiDetection) return
        IslandChangeEvent(island, oldIsland).post()
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Hypixel Mod API")
        event.addIrrelevant {
            addAll(debugData.map(::dataToString))
        }
    }

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) = reset()

    private fun resetWorldChange() {
        logger.log("World Change")
        island = IslandType.NONE
        inSkyblock = false
        serverId = null
        serverType = null
        mode = null
        map = null
        isGuest = false
        sentIslandEvent = false
        internalIsland = IslandType.NONE
    }

    private fun reset() {
        logger.log("Disconnected")

        val oldIsland = island
        island = IslandType.NONE
        if (oldIsland != island) {
            if (isModApiDetection) IslandChangeEvent(island, oldIsland).post()
        }

        val oldInHypixel = inHypixel
        inHypixel = false
        if (oldInHypixel != inHypixel) {
            if (isModApiDetection) HypixelLeaveEvent.post()
        }

        val oldSkyblock = inSkyblock
        inSkyblock = false
        if (oldSkyblock != inSkyblock) {
            if (isModApiDetection) SkyBlockLeaveEvent.post()
        }

        serverId = null
        inAlpha = false
        serverType = null
        mode = null
        map = null
        isGuest = false
        sentIslandEvent = false
        internalIsland = IslandType.NONE
    }

    private val debugData
        get() = arrayOf(
            "HypixelData.skyBlock" to HypixelData.skyBlock,
            "inSkyblock" to inSkyblock,
            "HypixelData.hypixelLive" to HypixelData.hypixelLive,
            "inHypixel" to inHypixel,
            "HypixelData.skyBlockIsland" to HypixelData.skyBlockIsland,
            "island" to island,
            "HypixelData.serverId" to HypixelData.serverId,
            "serverId" to serverId,
            "serverType" to serverType,
            "map" to map,
        )

    private fun dataToString(pair: Pair<String, Any?>) = "${pair.first}: ${pair.second}"

    private data class HypixelModApiJson(
        @Expose val enabled: Boolean = false,
    )

    var isModApiDetection: Boolean = false
        get() = field && config && SkyHanniMod.isBetaVersion
        private set

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val constant = event.getConstantOrDefault<HypixelModApiJson>("HypixelModApi") { HypixelModApiJson() }
        isModApiDetection = constant.enabled
    }

}
