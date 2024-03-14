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

    private var dwarvenEvents = listOf<RunningEventType>()
    private var crystalEvents = listOf<RunningEventType>()
    private var lastDwarvenEvent: MiningEventType? = null
    private var lastCrystalEvent: MiningEventType? = null

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
        updateEvents(IslandType.DWARVEN_MINES, dwarvenEvents, lastDwarvenEvent)
        updateEvents(IslandType.CRYSTAL_HOLLOWS, crystalEvents, lastCrystalEvent)
    }

    private fun updateEvents(islandType: IslandType, events: List<RunningEventType>, lastEvent: MiningEventType?) {
        val shouldShow = when (config.showType) {
            MiningEventConfig.ShowType.DWARVEN -> islandType == IslandType.DWARVEN_MINES
            MiningEventConfig.ShowType.CRYSTAL -> islandType == IslandType.CRYSTAL_HOLLOWS
            MiningEventConfig.ShowType.CURRENT -> islandType.isInIsland()
            else -> true
        }

        events.firstOrNull()?.let { firstEvent ->
            if (firstEvent.endsAt.asTimeMark().isInPast()) {
                when (islandType) {
                    IslandType.DWARVEN_MINES -> lastDwarvenEvent = firstEvent.event
                    IslandType.CRYSTAL_HOLLOWS -> lastCrystalEvent = firstEvent.event
                    else -> Unit
                }
            }
        }

        if (shouldShow) {
            val upcomingEvents = formatUpcomingEvents(events, lastEvent)
            display.add("§a${islandType.displayName}§8: $upcomingEvents")
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
        eventData.runningEvents.forEach { (islandType, events) ->
            when (islandType) {
                IslandType.DWARVEN_MINES -> dwarvenEvents =
                    events.sortedBy { it.endsAt - it.event.defaultLength.inWholeMilliseconds }

                IslandType.CRYSTAL_HOLLOWS -> crystalEvents =
                    events.filter { !it.event.dwarvenSpecific }
                        .sortedBy { it.endsAt - it.event.defaultLength.inWholeMilliseconds }
                else -> Unit
            }
        }
    }

    private fun shouldDisplay() = LorenzUtils.inSkyBlock && config.enabled && !ReminderUtils.isBusy() &&
        !(!config.outsideMining && !LorenzUtils.inAdvancedMiningIsland())
}
