package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object EquipmentWardrobeApi : WardrobeApi() {

    private val patternGroup = RepoPattern.group("inventory.wardrobe")

    /**
     * REGEX-TEST: (1/2) Equipment Sets
     */
    override val inventoryPattern by patternGroup.pattern(
        "inventory.name.equipment",
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
    fun onInventoryClose(event: InventoryCloseEvent) = handleInventoryClose()

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) = handleDebugDataCollect(event)
}
