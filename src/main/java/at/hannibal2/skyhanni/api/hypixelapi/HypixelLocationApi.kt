package at.hannibal2.skyhanni.api.hypixelapi

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelLeaveEvent
import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiJoinEvent
import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiServerChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.ScoreboardTitleUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.hypixel.data.type.GameType
import net.hypixel.data.type.ServerType

@Suppress("MemberVisibilityCanBePrivate")
@SkyHanniModule
object HypixelLocationApi {

    private val patternGroup = RepoPattern.group("api.hypixellocation")

    private val lobbyTypePattern by patternGroup.pattern(
        "lobbytype",
        "(?<lobbyType>.*lobby)\\d+",
    )

    var inHypixel: Boolean = false
        private set

    var inSkyblock: Boolean = false
        private set

    var island: IslandType = IslandType.NONE
        private set

    var serverId: String? = null
        private set

    val serverName get() = serverId.orEmpty()

    var inAlpha: Boolean = false
        private set

    var serverType: ServerType? = null
        private set

    var mode: String? = null
        private set

    var map: String? = null
        private set

    var lobbyName: String? = null
        private set

    var lobbyType: String? = null
        private set

    val inLobby get() = !lobbyName.isNullOrEmpty()
    val inLimbo get() = serverId == "limbo"

    var isGuest: Boolean = false
        private set

    private val logger = SkyHanniLogger("debug/hypixel_api")

    private var sentIslandEvent = false
    private var internalIsland = IslandType.NONE
    private var previousIsland = IslandType.NONE

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onHypixelJoin(event: HypixelApiJoinEvent) {
        logger.log(event.toString())
        logger.log("Connected to Hypixel")
        inAlpha = event.alpha
        val wasInHypixel = inHypixel
        inHypixel = true
        if (!wasInHypixel) {
            HypixelJoinEvent.post()
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onServerChange(event: HypixelApiServerChangeEvent) {
        logger.log(event.toString())
        inHypixel = true
        inSkyblock = event.serverType == GameType.SKYBLOCK
        serverType = event.serverType
        mode = event.mode
        map = event.map
        serverId = event.serverName
        lobbyName = event.lobbyName
        lobbyType = event.lobbyName?.let { name ->
            lobbyTypePattern.matchMatcher(name) { group("lobbyType") }
        }
        isGuest = false

        // Set island to NONE when you leave skyblock
        if (!inSkyblock) {
            internalIsland = IslandType.NONE
            changeIsland()
            return
        }
        val mode = event.mode ?: return

        val newIsland = IslandType.getByIdOrUnknown(mode)
        if (newIsland == IslandType.UNKNOWN) {
            ChatUtils.debug("Unknown island mode detected: '$mode'")
            logger.log("Unknown island mode detected: '$mode'")
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

    private fun changeIsland() {
        if (internalIsland == island) return
        val oldIsland = island
        island = internalIsland
        logger.log("Island change: '$oldIsland' -> '$island'")

        if (oldIsland != IslandType.NONE) {
            IslandLeaveEvent(oldIsland).post()
        }
        if (island != IslandType.NONE) {
            IslandJoinEvent(island = island, previousIsland = previousIsland).post()
            previousIsland = island
        }

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
    fun onDisconnect(event: ClientDisconnectEvent) {
        if (inSkyblock || island != IslandType.NONE) {
            internalIsland = IslandType.NONE
            changeIsland()
        }
        val wasInHypixel = inHypixel
        reset()
        if (wasInHypixel) {
            HypixelLeaveEvent.post()
        }
    }

    private fun reset() {
        logger.log("Disconnected")
        inHypixel = false
        inSkyblock = false
        island = IslandType.NONE
        serverId = null
        inAlpha = false
        serverType = null
        mode = null
        map = null
        lobbyName = null
        lobbyType = null
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
            "lobbyName" to lobbyName,
            "lobbyType" to lobbyType,
            "map" to map,
        )

    private fun dataToString(pair: Pair<String, Any?>) = "${pair.first}: ${pair.second}"

}
