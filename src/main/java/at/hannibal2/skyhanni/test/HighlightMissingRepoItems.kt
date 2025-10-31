package at.hannibal2.hanni.test

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.inventory.Slot

@HanniModule
object HighlightMissingRepoItems {

    @HandleEvent(priority = HandleEvent.LOWEST, onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!HanniMod.feature.dev.debug.highlightMissingRepo) return

        val gui = event.gui

        if (gui is GuiChest) {
            highlightItems(event.container.inventorySlots)
        } else if (gui is GuiInventory) {
            highlightItems(InventoryUtils.getSlotsInOwnInventory())
        }
    }

    private fun highlightItems(slots: Iterable<Slot>) {
        if (NeuItems.allInternalNames.isEmpty()) return
        for (slot in slots) {
            val internalName = slot.stack?.getInternalNameOrNull() ?: continue
            val asString = internalName.asString()
            if (asString.startsWith("BUILDER_")) continue // Skip builder items as we filter them out of allInternalNames

            if (NeuItems.ignoreItemsFilter.match(asString)) continue
            if (NeuItems.allInternalNames[asString] != null) continue

            slot.highlight(LorenzColor.RED)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.highlightMissingRepo", "dev.debug.highlightMissingRepo")
    }
}
