package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MaxwellAPI.favoritePowers
import at.hannibal2.skyhanni.data.MaxwellAPI.isThaumaturgyInventory
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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

    private var highlightedSlots = setOf<Int>()
    private var inInventory = false

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || !inInventory) return

        highlightedSlots.forEach { event.gui.inventorySlots.inventorySlots[it] highlight LorenzColor.AQUA }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !KeyboardManager.isShiftKeyDown() || !inInventory) return

        val displayName = event.item?.name?.removeColor()?.trim() ?: return
        val power = MaxwellAPI.getPowerByNameOrNull(displayName) ?: return

        if (power in favoritePowers) {
            favoritePowers -= power
            highlightedSlots -= event.slotId
        } else {
            favoritePowers += power
            highlightedSlots += event.slotId
        }

        event.cancel()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (!isThaumaturgyInventory(event.inventoryName)) return

        inInventory = true
        highlightedSlots = setOf()
        event.inventoryItems.forEach { (slot, item) ->
            if (item.displayName in favoritePowers) highlightedSlots += slot
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    private fun isEnabled() =
        LorenzUtils.inSkyBlock && storage != null && config.favoritePowerStone
}
