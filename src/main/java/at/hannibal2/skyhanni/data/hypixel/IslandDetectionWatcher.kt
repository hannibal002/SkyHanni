package at.hannibal2.skyhanni.data.hypixel

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
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
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.allIdentical
import at.hannibal2.skyhanni.utils.collection.SizeLimitedCache
import at.hannibal2.skyhanni.utils.collection.SizeLimitedSet
import kotlin.time.Duration.Companion.seconds

/**
 * Sends errors when the island state is different between tab list and infos from IslandChangeEvent
 */
@SkyHanniModule
object IslandDetectionWatcher {

    private val action = SizeLimitedSet<Data>(20)

    private var latestEventType = IslandType.NONE
    private var lastWorldChange = SimpleTimeMark.farFuture()
    private var showedError = false

    data class Data(val text: String, val time: SimpleTimeMark)

    private val logger = LorenzLogger("debug/island_change")

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        val oldIsland = event.oldIsland
        val newIsland = event.newIsland
        if (newIsland == IslandType.UNKNOWN) {
            val foundIsland = HypixelData.rawTabListIslandName()
            ChatUtils.debug("Unknown island detected: '$foundIsland'")
        }
        log("IslandChangeEvent $oldIsland -> $newIsland")
        latestEventType = newIsland
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

    private fun log(text: String) {
        action.add(Data(text, SimpleTimeMark.now()))
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

        val tabListType = HypixelData.fetchTabListType()
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
            "Wanna have a one time workaround for now? Click here!",
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
        HypixelData.workaroundChangeTo(type)
        ChatUtils.chat("Changed island type to ${type.displayName} as a workaround")
    }

    private fun buildLog(): List<String> = action.map { data ->
        val time = data.time
        "${data.text} (${time.passedSince()} ago, $time)"
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Island Detection Watcher")

        val tabListType = HypixelData.fetchTabListType()
        val currentEventType = latestEventType
        val internalType = SkyBlockUtils.currentIsland

        val isCurrentlyValid = listOf(tabListType, currentEventType, internalType).allIdentical()
        val isRelevant = !isCurrentlyValid || showedError

        val list = buildList {
            add("isCurrentlyValid: $isCurrentlyValid")
            add("error got shown: $showedError")
            add(" ")
            add("tabListType: $tabListType")
            add("currentEventType: $currentEventType")
            add("internalType: $internalType")
            add(" ")
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
