package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.world.item.Items

@SkyHanniModule
object HarvestableHighlight {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightHarvestableStatus) return
        if (!GreenhouseUtils.cropDiagnosticInventory.isInside()) return
        val slot = InventoryUtils.getSlotAtIndex(24) ?: return
        val beacon = slot.item ?: return
        if (beacon.item != Items.BEACON) return
        var color = LorenzColor.RED
        for (component in beacon.getLoreComponent()) {
            val line = component.string
            if (line.contains("Status: ")) {
                if (line == "Status: Harvestable") {
                    color = LorenzColor.GREEN
                    continue
                }
                if (line.startsWith("Drops: ") || line.startsWith("Rewards: ")) {
                    color = LorenzColor.YELLOW
                    continue
                }
            }
        }
        slot.highlight(color)
    }
}
