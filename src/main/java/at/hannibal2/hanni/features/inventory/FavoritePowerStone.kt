package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.MaxwellApi
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryOpenEvent
import at.hannibal2.hanni.events.InventoryUpdatedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.KeyboardManager
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object FavoritePowerStone {

    private val config get() = HanniMod.feature.inventory
    private val storage get() = ProfileStorageData.profileSpecific

    private var highlightedSlots = setOf<Int>()
    private var inInventory = false

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || !inInventory) return

        highlightedSlots.forEach { event.container.inventorySlots[it].highlight(LorenzColor.AQUA) }
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !KeyboardManager.isShiftKeyDown() || !inInventory) return

        val displayName = event.item?.displayName?.removeColor()?.trim() ?: return
        val power = MaxwellApi.getPowerByNameOrNull(displayName) ?: return

        if (power in MaxwellApi.favoritePowers) {
            MaxwellApi.favoritePowers -= power
            highlightedSlots -= event.slotId
        } else {
            MaxwellApi.favoritePowers += power
            highlightedSlots += event.slotId
        }

        event.cancel()
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled() || !MaxwellApi.isThaumaturgyInventory(event.inventoryName)) return

        inInventory = true
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled() || !inInventory) return

        highlightedSlots = event.inventoryItems
            .filter { (_, item) -> item.displayName.removeColor() in MaxwellApi.favoritePowers }
            .keys
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && storage != null && config.favoritePowerStone
}
