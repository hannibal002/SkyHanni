package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.CurrentEquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SafeItemStack

@SkyHanniModule
object EquipmentWardrobeApi : AbstractWardrobeApi() {

    /**
     * REGEX-TEST: (1/2) Equipment Sets
     */
    override val inventoryPattern by patternGroup.pattern(
        "equipment.name",
        "\\((?<currentPage>\\d+)/\\d+\\) Equipment Sets",
    )

    override val valueName = "Equipment"
    override val debugTitle = "Equipment Wardrobe"

    override val storage get() = ProfileStorageData.profileSpecific?.equipmentWardrobe

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        handleInventoryOpen(event.inventoryName)
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) = handleInventoryUpdated(event)

    @HandleEvent
    fun onInventoryClose() = handleInventoryClose()

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) = handleDebugDataCollect(event)

    override fun onEquippedSlotUpdated(items: List<SafeItemStack?>) {
        EquipmentSlot.entries.forEach { CurrentEquipmentApi.setEquipment(it, items[it.ordinal]) }
    }
}
