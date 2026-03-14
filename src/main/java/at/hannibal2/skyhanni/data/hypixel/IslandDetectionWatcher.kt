package at.hannibal2.skyhanni.data.hypixel

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.skyblock.SkyBlockLeaveEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.allIdentical
import at.hannibal2.skyhanni.utils.collection.SizeLimitedCache
import kotlin.time.Duration.Companion.seconds

/**
 * Sends errors when the island state is different between tab list and infos from IslandChangeEvent
 */
@SkyHanniModule
object IslandDetectionWatcher {

    private val action = SizeLimitedCache<String, SimpleTimeMark>(20)

    private var latestEventType = IslandType.NONE
    private var lastWorldChange = SimpleTimeMark.farFuture()
    private var showedError = false

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

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        log("ProfileJoinEvent")
    }

    @HandleEvent(HypixelJoinEvent::class)
    fun onHypixelJoin() {
        log("HypixelJoinEvent")
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
            "log" to buildLog(),
        )
        showedError = true
        suggestWorkaround(tabListType)
    }

    private fun suggestWorkaround(type: IslandType) {
        ChatUtils.clickableChat(
            "Wanna have a one time workaround for now? Click here!",
            onClick = { doWorkaround(type) },
            hover = "Click to change the internal island type to ${type.displayName}",
        )
    }

    private fun doWorkaround(type: IslandType) {
        if (!SkyBlockUtils.inSkyBlock) {
            ChatUtils.userError("this only works while on SkyBlock!")
            return
        }
        log("workaround to $type")
        HypixelData.workaroundChangeTo(type)
        ChatUtils.chat("Changed island type to ${type.displayName} as a workaround")
    }

    private fun fetchTabListType(): IslandType {
        val guesting = HypixelData.guestPattern.matches(ScoreboardData.objectiveTitle.removeColor())
        val foundIsland = TabWidget.AREA.matchMatcherFirstLine { group("island").removeColor() }.orEmpty()
        return HypixelData.getIslandType(foundIsland, guesting)
    }

    private fun buildLog(): List<String> {
        val result = mutableListOf<String>()
        for ((text, time) in action) {
            result.add("$text (${time.passedSince()} ago, $time)")
        }
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
            for (line in buildLog()) {
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
