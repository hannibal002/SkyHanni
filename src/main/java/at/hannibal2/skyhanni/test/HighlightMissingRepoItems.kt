package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.inventory.Slot

@SkyHanniModule
object HighlightMissingRepoItems {

    @HandleEvent(priority = HandleEvent.LOWEST, onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.dev.debug.highlightMissingRepo) return

        val gui = event.gui

        if (gui is GuiChest) {
            highlightItems(event.context, event.container.inventorySlots)
        } else if (gui is GuiInventory) {
            highlightItems(event.context, InventoryUtils.getSlotsInOwnInventory())
        }
    }

    private fun highlightItems(context: DrawContext, slots: Iterable<Slot>) {
        if (NeuItems.allInternalNames.isEmpty()) return
        for (slot in slots) {
            val internalName = slot.stack?.getInternalNameOrNull() ?: continue

            if (NeuItems.ignoreItemsFilter.match(internalName.asString())) continue
            if (NeuItems.allInternalNames.contains(internalName)) continue

            slot.highlight(context, LorenzColor.RED)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.highlightMissingRepo", "dev.debug.highlightMissingRepo")
    }
}
