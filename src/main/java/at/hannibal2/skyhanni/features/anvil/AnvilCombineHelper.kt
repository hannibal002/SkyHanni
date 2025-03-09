package at.hannibal2.skyhanni.features.anvil

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.inventory.AnvilUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.item.ItemStack

@SkyHanniModule
object AnvilCombineHelper {

    private var highlightedSlots = setOf<Int>()

    @HandleEvent(priority = HandleEvent.LOW)
    fun onInventoryUpdated(event: AnvilUpdateEvent) {
        highlightedSlots = updateSlots(event.left, event.right)
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        highlightedSlots = updateSlots(AnvilApi.left, AnvilApi.right)
    }

    private fun updateSlots(left: ItemStack?, right: ItemStack?): Set<Int> {
        if (!isEnabled()) return emptySet()

        val leftStack = left?.getInternalName()
        val rightStack = right?.getInternalName()

        // don't highlight if both slots have items
        if (leftStack != null && rightStack != null) return emptySet()

        // don't highlight if both slots have no items
        if (leftStack == null && rightStack == null) return emptySet()

        return InventoryUtils.getSlotsInOwnInventory().filter { slot ->
            val name = slot.stack?.getInternalName()
            name == leftStack || name == rightStack
        }.map { it.slotNumber }.toSet()

    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        for (slot in InventoryUtils.getSlotsInOwnInventory()) {
            if (slot.slotNumber in highlightedSlots) {
                slot highlight LorenzColor.GREEN
            }
        }
    }

    fun isEnabled() = SkyHanniMod.feature.inventory.anvilCombineHelper && AnvilApi.inventory.isInside()
}
