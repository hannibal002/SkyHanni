package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object ArmorWardrobeApi : AbstractWardrobeApi() {

    /**
     * REGEX-TEST: (1/3) Armor Sets
     */
    override val inventoryPattern by patternGroup.pattern(
        "armor.name",
        "\\((?<currentPage>\\d+)/\\d+\\) Armor Sets",
    )

    override val valueName = "Armor"
    override val debugTitle = "Wardrobe"

    override val storage get() = ProfileStorageData.profileSpecific?.wardrobe

    @HandleEvent
    private fun onInventoryOpen(event: InventoryOpenEvent) {
        handleInventoryOpen(event.inventoryName)
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) = handleInventoryUpdated(event)

    @HandleEvent
    private fun onInventoryClose(event: InventoryCloseEvent) = handleInventoryClose()

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) = handleDebugDataCollect(event)

    // This also modifies the "inWardrobe" property
    internal fun matchesInventoryName(inventoryName: String): Boolean {
        return handleInventoryOpen(inventoryName)
    }
}
