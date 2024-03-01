package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.mining.MiningEventConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MiningEventDisplay {
    private val config get() = SkyHanniMod.feature.mining.miningEvent
    private var display = mutableListOf<String>()

    private val dwarvenEvents = mutableListOf<RunningEvent>()
    private val crystalEvents = mutableListOf<RunningEvent>()
    private var lastDwarvenEvent: MiningEvent? = null
    private var lastCrystalEvent: MiningEvent? = null

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

    private fun updateEvents(islandType: IslandType, events: List<RunningEvent>, lastEvent: MiningEvent?) {
        val shouldShow = when (config.showType) {
            MiningEventConfig.ShowType.DWARVEN -> islandType == IslandType.DWARVEN_MINES
            MiningEventConfig.ShowType.CRYSTAL -> islandType == IslandType.CRYSTAL_HOLLOWS
            MiningEventConfig.ShowType.CURRENT -> islandType.isInIsland()
            else -> true
        }

        events.firstOrNull { it.endsAt.asTimeMark().isInPast() }?.let {
            when (islandType) {
                IslandType.DWARVEN_MINES -> lastDwarvenEvent = it.event
                IslandType.CRYSTAL_HOLLOWS -> lastCrystalEvent = it.event
                else -> Unit
            }
        }

        if (shouldShow) {
            val upcomingEvents = formatUpcomingEvents(events, lastEvent)
            display.add("§a${islandType.displayName}§7: $upcomingEvents")
        }
    }

    private fun formatUpcomingEvents(events: List<RunningEvent>, lastEvent: MiningEvent?): String {
        val upcoming = events.filter { !it.endsAt.asTimeMark().isInPast() }
            .map { if (it.isDouble) "${it.event} §7-> ${it.event}" else it.event.toString() }
            .toMutableList()

        if (upcoming.isEmpty()) upcoming.add("§7???")
        lastEvent?.let { upcoming.add(0, it.toPastString()) }
        return upcoming.joinToString(" §7-> ")
    }

    fun updateData(eventData: MiningEventData) {
        dwarvenEvents.clear()
        crystalEvents.clear()

        eventData.runningEvents.forEach { (islandType, events) ->
            when (islandType) {
                IslandType.DWARVEN_MINES -> dwarvenEvents.addAll(events)
                IslandType.CRYSTAL_HOLLOWS -> crystalEvents.addAll(events)
                else -> Unit
            }
        }
    }

    private fun shouldDisplay() = LorenzUtils.inSkyBlock && config.enabled && !ReminderUtils.isBusy() &&
        !(!config.outsideMining && !LocationUtils.inAdvancedMiningIsland())
}
