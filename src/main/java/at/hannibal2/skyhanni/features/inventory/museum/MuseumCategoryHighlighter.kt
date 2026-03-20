package at.hannibal2.skyhanni.features.inventory.museum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object MuseumCategoryHighlighter {

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.inventory.museumCategoryHighlight) return
        if (InventoryUtils.openInventoryName() != "Your Museum") return
        for (slot in event.container.slots) {
            val lore = slot.item.getLoreComponent()
            for (line in lore) {
                if (line.string == "Items Donated: 100%") slot.highlight(LorenzColor.GREEN)
            }
        }
    }

}