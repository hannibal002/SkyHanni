package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object FavoritePowerStone {

    private val config get() = SkyHanniMod.feature.inventory
    private val storage get() = ProfileStorageData.profileSpecific

    private var highlightedSlots = mutableSetOf<Int>()

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!MaxwellAPI.isThaumaturgyInventory(InventoryUtils.openInventoryName())) return

        highlightedSlots.forEach { event.gui.inventorySlots.inventorySlots[it] highlight LorenzColor.AQUA }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !KeyboardManager.isShiftKeyDown()) return
        if (!MaxwellAPI.isThaumaturgyInventory(InventoryUtils.openInventoryName())) return

        val displayName = event.item?.name?.removeColor()?.trim() ?: return
        val power = MaxwellAPI.getPowerByNameOrNull(displayName) ?: return

        if (power in favoritePowers) {
            MaxwellAPI.favoritePowers -= power
            highlightedSlots -= event.slotId
        } else {
            MaxwellAPI.favoritePowers += power
            highlightedSlots += event.slotId
        }

        event.cancel()
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (!MaxwellAPI.isThaumaturgyInventory(event.inventoryName)) return

        event.inventoryItems.forEach { (slot, item) ->
            if (item.displayName in MaxwellAPI.favoritePowers) highlightedSlots += slot
        }
    }

    private fun isEnabled() =
        LorenzUtils.inSkyBlock && storage != null && config.favoritePowerStone
}
