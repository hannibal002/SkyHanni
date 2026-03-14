package at.hannibal2.skyhanni.data.hypixel

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.skyblock.SkyBlockLeaveEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.allIdentical
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Sends errors when the island state is different between tab list and infos from IslandChangeEvent
 */
@SkyHanniModule
object IslandDetectionWatcher {

    private val action = mutableMapOf<String, SimpleTimeMark>()

    private var latestEventType = IslandType.NONE
    private var lastWorldChange = SimpleTimeMark.farFuture()
    private var showedError = false
    private var lastActionLog = listOf<String>()

    private val logger = LorenzLogger("debug/island_change")

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        val old = event.oldIsland
        val new = event.newIsland
        log("IslandChangeEvent $old -> $new")

        latestEventType = new
    }

    @HandleEvent(SkyBlockLeaveEvent::class)
    fun onSkyBlockLeave() {
        log("SkyBlockLeaveEvent")
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        log("WorldChangeEvent")
        lastWorldChange = SimpleTimeMark.now()
        showedError = false
    }

    fun log(text: String) {
        action[text] = SimpleTimeMark.now()
        logger.log(text)
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (lastWorldChange.passedSince() > 10.seconds) {
            lastWorldChange = SimpleTimeMark.farFuture()
            checkIslandConsistency()
        }
    }

    private fun checkIslandConsistency() {
        if (!SkyBlockUtils.inSkyBlock) return

        val tabListType = fetchTabListType()
        val latestEventType = latestEventType
        val internalType = SkyBlockUtils.currentIsland

        if (listOf(tabListType, latestEventType, internalType).allIdentical()) return

        ErrorManager.logErrorStateWithData(
            userMessage = "Error loading island type",
            internalMessage = "invalid island state",
            "tab list" to tabListType,
            "latest event" to latestEventType,
            "internal" to internalType,
            "log" to buildAndClearLog(),
        )
        showedError = true
    }

    private fun fetchTabListType(): IslandType {
        val guesting = HypixelData.guestPattern.matches(ScoreboardData.objectiveTitle.removeColor())
        val foundIsland = TabWidget.AREA.matchMatcherFirstLine { group("island").removeColor() }.orEmpty()
        return HypixelData.getIslandType(foundIsland, guesting)
    }

    private fun buildAndClearLog(): List<String> {
        val result = mutableListOf<String>()
        for ((text, time) in action) {
            if (time.passedSince() > 5.minutes) continue

            result.add("$text (${time.passedSince()} ago, $time)")
        }
        action.clear()

        lastActionLog = result
        return result
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Island Detection Watcher")

        val tabListType = fetchTabListType()
        val latestEventType = latestEventType
        val internalType = SkyBlockUtils.currentIsland

        val list = buildList {
            add("showedError: $showedError")
            add(" ")
            add("tabListType: $tabListType")
            add("latestEventType: $latestEventType")
            add("internalType: $internalType")
            add(" ")
            add("log: ")
            for (line in lastActionLog) {
                add(" - $line")
            }
        }

        if (showedError) {
            event.addData(list)
        } else {
            event.addIrrelevant(list)
        }
    }
}
