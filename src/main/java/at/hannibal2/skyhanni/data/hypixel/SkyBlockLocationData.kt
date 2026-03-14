package at.hannibal2.skyhanni.data.hypixel

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * This class handles the "am I in SkyBlock" and "what SkyBlock island am I on" checks.
 * For "Am i on hypixel" see [HypixelData].
 */
@SkyHanniModule
object SkyBlockLocationData {

    private val patternGroup = RepoPattern.group("data.skyblockstate")

    /**
     * REGEX-TEST: SKYBLOCK
     * REGEX-TEST: SKYBLOCK GUEST
     * REGEX-TEST: SKYBLOCK CO-OP
     * REGEX-TEST: SKYBLOCK ♲
     * REGEX-TEST: SKYBLOCK ☀
     * REGEX-TEST: SKYBLOCK Ⓑ
     */
    private val scoreboardTitlePattern by patternGroup.pattern(
        "scoreboard.title",
        "SK[YI]BLOCK(?: CO-OP| GUEST)?(?: [♲☀Ⓑ])?",
    )

    private val guestPattern by patternGroup.pattern(
        "scoreboard.guest",
        "SKYBLOCK GUEST",
    )

    val inSkyBlock: Boolean get() = SkyBlockUtils.onHypixel && scoreboardShowsSkyBlock
    val currentIsland: IslandType get() = confirmedIsland

    private var scoreboardShowsSkyBlock = false
    private var tabListIsland = IslandType.NONE
    private var confirmedIsland = IslandType.NONE
    private var previousIsland = IslandType.NONE
    private var scoreboardTitle: String? = null

    @HandleEvent
    fun onWorldChange() {
        scoreboardTitle = null
        scoreboardShowsSkyBlock = false
        tabListIsland = IslandType.NONE
        handleStateChange()
    }

    @HandleEvent(ClientDisconnectEvent::class)
    fun onDisconnect() {
        scoreboardTitle = null
        scoreboardShowsSkyBlock = false
        tabListIsland = IslandType.NONE
        if (confirmedIsland != IslandType.NONE) {
            changeTo(IslandType.NONE)
        }
    }

    @HandleEvent(ScoreboardUpdateEvent::class)
    fun onScoreboardUpdate() {
        scoreboardTitle = HypixelData.getScoreboardTitle()?.removeColor()
        scoreboardShowsSkyBlock = scoreboardTitle?.let { scoreboardTitlePattern.matches(it) } ?: false
        handleStateChange()
    }

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (event.widget != TabWidget.AREA) return

        tabListIsland = if (event.isClear()) IslandType.NONE else fetchTabListType()
        handleStateChange()
    }

    // Can not use color coding, because of the color effect (§f§lSKYB§6§lL§e§lOCK§A§L GUEST)
    fun fetchTabListType(): IslandType {
        val isGuest = scoreboardTitle?.let { guestPattern.matches(it) } ?: false
        val islandName = rawTabListIslandName()
        val islandType = IslandType.getByNameOrUnknown(islandName)
        return if (isGuest) islandType.guestVariant() else islandType
    }

    fun rawTabListIslandName(): String = TabWidget.AREA.matchMatcherFirstLine { group("island").removeColor() }.orEmpty()

    fun workaroundChangeTo(newIsland: IslandType) {
        ChatUtils.debug("workaroundChangeTo $newIsland")
        if (confirmedIsland != IslandType.NONE) {
            changeTo(IslandType.NONE)
        }
        tabListIsland = newIsland
        changeTo(newIsland)
    }

    private fun handleStateChange() {
        val newIsland = if (inSkyBlock) tabListIsland else IslandType.NONE
        if (newIsland == confirmedIsland) return

        changeTo(newIsland)
    }

    private fun changeTo(newIsland: IslandType) {
        if (confirmedIsland == newIsland) {
            ErrorManager.logErrorStateWithData(
                "Invalid island type change detected",
                "old and new island are identical, this should never happen!",
                "newIsland" to newIsland,
            )
            return
        }
        val oldIsland = confirmedIsland
        confirmedIsland = newIsland

        if (newIsland == IslandType.NONE) {
            IslandLeaveEvent(oldIsland).post()
        }
        if (oldIsland == IslandType.NONE) {
            IslandJoinEvent(island = newIsland, previousIsland = previousIsland).post()
            previousIsland = newIsland
        }

        // TODO delete this event eventually
        IslandChangeEvent(newIsland, oldIsland).post()
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("SkyBlock Location Data")
        val list = listOf(
            "onHypixel: ${SkyBlockUtils.onHypixel}",
            "scoreboardShowsSkyBlock: $scoreboardShowsSkyBlock",
            "scoreboardTitle: $scoreboardTitle",
            "tabListIsland: $tabListIsland",
            "tab widget AREA lines: ${TabWidget.AREA.lines}",
            "confirmedIsland: $confirmedIsland",
            "inSkyBlock: $inSkyBlock",
            "fetchTabListType: ${fetchTabListType()}",
        )
        if (inSkyBlock) {
            event.addIrrelevant(list)
        } else {
            event.addData(list)
        }
    }
}
