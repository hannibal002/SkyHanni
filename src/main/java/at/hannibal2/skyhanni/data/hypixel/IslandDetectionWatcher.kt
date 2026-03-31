package at.hannibal2.skyhanni.data.hypixel

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.allIdentical
import at.hannibal2.skyhanni.utils.collection.SizeLimitedSet
import kotlin.time.Duration.Companion.seconds

/**
 * Sends errors when the island state is different between tab list and infos from IslandChangeEvent
 */
@SkyHanniModule
object IslandDetectionWatcher {

    private data class Action(val text: String, val time: SimpleTimeMark)

    private val action = SizeLimitedSet<Action>(20)

    private var latestEventType = IslandType.UNKNOWN
    private var lastWorldChange = SimpleTimeMark.farFuture()
    private var showedError = false

    private val logger = SkyHanniLogger("debug/island_change")

    @HandleEvent
    fun onIslandJoin(event: IslandJoinEvent) {
        val newIsland = event.island
        val oldIsland = event.previousIsland
        if (newIsland == IslandType.UNKNOWN) {
            val foundIsland = SkyBlockLocationData.rawTabListIslandName()
            ChatUtils.debug("Unknown island detected: '$foundIsland'")
        }
        log("IslandJoinEvent $oldIsland -> $newIsland")
        latestEventType = newIsland
    }

    @HandleEvent
    fun onIslandLeave(event: IslandLeaveEvent) {
        log("IslandLeaveEvent ${event.island}")
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

    private fun log(text: String) {
        action.add(Action(text, SimpleTimeMark.now()))
        logger.log(text)
        // TODO add ChatUtils.debug here once DevApi is advanced enough
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

        val tabListType = SkyBlockLocationData.fetchTabListType()
        val currentEventType = latestEventType
        val internalType = SkyBlockUtils.currentIsland

        if (listOf(tabListType, currentEventType, internalType).allIdentical()) return

        ErrorManager.logErrorStateWithData(
            userMessage = "Error loading island type",
            internalMessage = "invalid island state",
            "tab list" to tabListType,
            "latest event" to currentEventType,
            "internal" to internalType,
            "log" to buildLog(),
        )
        showedError = true
        suggestWorkaround(tabListType)
    }

    private fun suggestWorkaround(type: IslandType) {
        ChatUtils.clickableChat(
            "Wanna have a one-time workaround for now? Click here!",
            onClick = { doWorkaround(type) },
            hover = "Click to change the internal island type to ${type.displayName}",
        )
    }

    private fun doWorkaround(type: IslandType) {
        if (!SkyBlockUtils.inSkyBlock) {
            ChatUtils.userError("This only works while on SkyBlock!")
            return
        }
        log("workaround to $type")
        SkyBlockLocationData.workaroundChangeTo(type)
        ChatUtils.chat("Changed island type to ${type.displayName} as a workaround")
    }

    private fun buildLog(): List<String> = action.sortedBy { it.time }.map { (text, time) ->
        "$text ${time.passedSince().format()} ago($time)"
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Island Detection Watcher")

        val tabListType = SkyBlockLocationData.fetchTabListType()
        val currentEventType = latestEventType
        val internalType = SkyBlockUtils.currentIsland

        val isIncorrect = !listOf(tabListType, currentEventType, internalType).allIdentical()
        val isRelevant = isIncorrect || showedError

        val list = buildList {
            add("isWrong: $isIncorrect")
            add("error got shown: $showedError")
            add("")
            add("tabListType: $tabListType")
            add("currentEventType: $currentEventType")
            add("internalType: $internalType")
            add("")
            add("log: ")
            for (line in buildLog()) {
                add(" - $line")
            }
        }

        if (isRelevant) {
            event.addData(list)
        } else {
            event.addIrrelevant(list)
        }
    }
}
