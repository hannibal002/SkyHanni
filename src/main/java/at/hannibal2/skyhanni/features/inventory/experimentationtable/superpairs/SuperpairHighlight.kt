package at.hannibal2.skyhanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationSuperpairApi
import at.hannibal2.skyhanni.api.ExperimentationSuperpairApi.FoundType
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object SuperpairHighlight {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.superpairs

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightPairs) return
        if (!ExperimentationTableApi.inSuperpairs) return

        val collectedSlots = ExperimentationSuperpairApi.found(FoundType.PAIR).slotIds()
        val matchedSlots = ExperimentationSuperpairApi.found(FoundType.MATCH).slotIds()
        val seenSlots = ExperimentationSuperpairApi.found(FoundType.NORMAL).mapNotNull { it.item?.slotId }
        val powerupSlots = ExperimentationSuperpairApi.found(FoundType.POWERUP).mapNotNull { it.item?.slotId }
        if (collectedSlots.isEmpty() && matchedSlots.isEmpty() && seenSlots.isEmpty() && powerupSlots.isEmpty()) return

        InventoryUtils.getItemsInOpenChest().forEach { slot ->
            when (slot.index) {
                in collectedSlots -> slot.highlight(config.collectedColor)
                in matchedSlots -> slot.highlight(config.matchedColor)
                in seenSlots -> slot.highlight(config.seenColor)
                in powerupSlots -> slot.highlight(config.powerupColor)
            }
        }
    }

    private fun List<ExperimentationSuperpairApi.FoundData>.slotIds() = flatMap {
        listOfNotNull(it.first?.slotId, it.second?.slotId)
    }
}
