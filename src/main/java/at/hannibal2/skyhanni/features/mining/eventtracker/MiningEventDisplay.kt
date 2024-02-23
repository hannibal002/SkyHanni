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

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.repeatSeconds(1)) return
        updateDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!shouldDisplay()) return
        config.position.renderStrings(display, posLabel = "True Farming Fortune")
    }

    private fun updateDisplay() {
        val shouldShowDwarven = when {
            config.showType == MiningEventConfig.ShowType.BOTH -> true
            config.showType == MiningEventConfig.ShowType.DWARVEN -> true
            config.showType == MiningEventConfig.ShowType.CURRENT && IslandType.DWARVEN_MINES.isInIsland() -> true
            else -> false
        }
        val shouldShowCrystal = when {
            config.showType == MiningEventConfig.ShowType.BOTH -> true
            config.showType == MiningEventConfig.ShowType.CRYSTAL -> true
            config.showType == MiningEventConfig.ShowType.CURRENT && IslandType.CRYSTAL_HOLLOWS.isInIsland() -> true
            else -> false
        }

        display.clear()
        if (shouldShowDwarven) display.add("§aDwarven Mines: ${formUpcomingString(dwarvenEvents)}")
        if (shouldShowCrystal) display.add("§aCrystal Hollows: ${formUpcomingString(crystalEvents)}")
    }

    private fun formUpcomingString(eventList: MutableList<RunningEvent>): String {
        var output = ""
        var foundCount = 0
        for (event in eventList) {
            val endsIn = event.endsAt.asTimeMark()
            if (endsIn.isInPast()) continue

            output = when (foundCount) {
                0 -> event.event.toString()
                else -> "$output §7-> ${event.event}"
            }
            if (event.isDouble) {
                output = "$output §7-> ${event.event}"
                foundCount ++
            }
            foundCount ++
        }
        if (output.isEmpty()) return "§8NONE"
        return output
    }

    fun updateData(eventData: MiningEventData) {
        dwarvenEvents.clear()
        crystalEvents.clear()

        for ((islandType, events) in eventData.runningEvents) {
            when (islandType) {
                IslandType.DWARVEN_MINES -> dwarvenEvents.addAll(events)
                IslandType.CRYSTAL_HOLLOWS -> crystalEvents.addAll(events)
                else -> continue
            }
        }
    }

    private fun shouldDisplay(): Boolean {
        if (!LorenzUtils.inSkyBlock || !config.enabled || ReminderUtils.isBusy()) return false
        if (!config.outsideMining && !LocationUtils.inAdvancedMiningIsland()) return false
        return true
    }
}
