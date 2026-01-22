package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object HarvestableHighlight {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightHarvestableStatus) return
        if (!GreenhouseUtils.cropDiagnosticInventory.isInside()) return
        val slot = InventoryUtils.getSlotAtIndex(24) ?: return
        val beacon = slot.item ?: return
        for (component in beacon.getLoreComponent()) {
            if (component.string.contains("Status: ")) {
                if (component.string == "Status: Harvestable") {
                    slot.highlight(LorenzColor.GREEN)
                    return
                }
                slot.highlight(LorenzColor.RED)
                return
            }
        }

    }
}