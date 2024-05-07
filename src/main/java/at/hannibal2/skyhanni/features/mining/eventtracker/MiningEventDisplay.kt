package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.mining.MiningEventConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MiningEventDisplay {
    private val config get() = SkyHanniMod.feature.mining.miningEvent
    private var display = mutableListOf<String>()

    private val islandEventData: MutableMap<IslandType, MiningIslandEventInfo> = mutableMapOf()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.repeatSeconds(1)) return
        updateDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!shouldDisplay()) return
        config.position.renderStrings(display, posLabel = "Upcoming Events Display")
    }

    private fun updateDisplay() {
        display.clear()
        updateEvents()
    }

    private fun updateEvents() {
        for ((islandType, eventDetails) in islandEventData) {
            val shouldShow = when (config.showType) {
                MiningEventConfig.ShowType.DWARVEN -> islandType == IslandType.DWARVEN_MINES
                MiningEventConfig.ShowType.CRYSTAL -> islandType == IslandType.CRYSTAL_HOLLOWS
                MiningEventConfig.ShowType.MINESHAFT -> islandType == IslandType.MINESHAFT
                MiningEventConfig.ShowType.CURRENT -> islandType.isInIsland()
                else -> true
            }

            eventDetails.islandEvents.firstOrNull()?.let { firstEvent ->
                if (firstEvent.endsAt.asTimeMark().isInPast()) {
                    eventDetails.lastEvent = firstEvent.event
                }
            }

            if (shouldShow) {
                val upcomingEvents = formatUpcomingEvents(eventDetails.islandEvents, eventDetails.lastEvent)
                display.add("§a${islandType.displayName}§8: $upcomingEvents")
            }
        }
    }

    private fun formatUpcomingEvents(events: List<RunningEventType>, lastEvent: MiningEventType?): String {
        val upcoming = events.filter { !it.endsAt.asTimeMark().isInPast() }
            .map { if (it.isDoubleEvent) "${it.event} §8-> ${it.event}" else it.event.toString() }
            .toMutableList()

        if (upcoming.isEmpty()) upcoming.add("§7???")
        if (config.passedEvents && upcoming.size < 4) lastEvent?.let { upcoming.add(0, it.toPastString()) }
        return upcoming.joinToString(" §8-> ")
    }

    fun updateData(eventData: MiningEventData) {
        for ((islandType, events) in eventData.runningEvents) {
            val sorted = events.filter { islandType == IslandType.DWARVEN_MINES || !it.event.dwarvenSpecific }
                .sortedBy { it.endsAt - it.event.defaultLength.inWholeMilliseconds }

            val oldData = islandEventData[islandType]
            if (oldData == null) {
                //todo remove once mineshaft is on main server
                if (sorted.isNotEmpty() || islandType != IslandType.MINESHAFT) {
                    islandEventData[islandType] = MiningIslandEventInfo(sorted)
                }
            } else {
                oldData.islandEvents = sorted
            }
        }
    }

    private fun shouldDisplay() = LorenzUtils.inSkyBlock && config.enabled && !ReminderUtils.isBusy() &&
        !(!config.outsideMining && !LorenzUtils.inAdvancedMiningIsland())
}

private class MiningIslandEventInfo(var islandEvents: List<RunningEventType>, var lastEvent: MiningEventType? = null)
