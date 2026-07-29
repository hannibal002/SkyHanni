package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import kotlin.collections.forEach

@SkyHanniModule
object EquipmentWardrobeApi : WardrobeApi() {

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
    fun onInventoryClose(event: InventoryCloseEvent) {
        handleInventoryClose()

        val currentEquipped = currentSlot?.let {
            slots[it]
        }?.getData()?.armor ?: return
        EquipmentSlot.entries.forEach {
            val itemStack = currentEquipped[it.ordinal]
            if (itemStack != null && !itemStack.isStainedGlassPane()) {
                EquipmentApi.setEquipment(it, itemStack)
            } else EquipmentApi.setEquipment(it, null)
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) = handleDebugDataCollect(event)
}
