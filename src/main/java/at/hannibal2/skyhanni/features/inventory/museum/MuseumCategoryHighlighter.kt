package at.hannibal2.skyhanni.features.inventory.museum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object MuseumCategoryHighlighter {

    private val museumDetector = InventoryDetector { name -> name == "Your Museum" }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.inventory.museumCategoryHighlight) return
        if (!museumDetector.isInside()) return
        for (slot in event.container.slots) {
            val lore = slot.item.getLoreComponent()
            if (lore.any { it.string == "Items Donated: 100%" }) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }
}
