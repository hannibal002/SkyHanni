package at.hannibal2.skyhanni.data.hypixel

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.SizeLimitedSet

/**
 * Logs the location event flow for debugging island changes.
 */
@SkyHanniModule
object IslandDetectionWatcher {

    private data class Action(val text: String, val time: SimpleTimeMark)

    private val action = SizeLimitedSet<Action>(20)

    private var latestEventType = IslandType.UNKNOWN

    private val logger = SkyHanniLogger("debug/island_change")

    @HandleEvent
    fun onIslandJoin(event: IslandJoinEvent) {
        val newIsland = event.island
        val oldIsland = event.previousIsland
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

    private fun buildLog(): List<String> = action.sortedBy { it.time }.map { (text, time) ->
        "$text ${time.passedSince().format()} ago($time)"
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Island Detection Watcher")

        val currentEventType = latestEventType
        val internalType = SkyBlockUtils.currentIsland

        val list = buildList {
            add("currentEventType: $currentEventType")
            add("internalType: $internalType")
            add("")
            add("log: ")
            for (line in buildLog()) {
                add(" - $line")
            }
        }

        event.addIrrelevant(list)
    }
}
