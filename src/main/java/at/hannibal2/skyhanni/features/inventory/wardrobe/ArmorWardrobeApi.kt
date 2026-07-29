package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object ArmorWardrobeApi : WardrobeApi() {

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

    var inCustomWardrobe = false

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val matched = handleInventoryOpen(event.inventoryName)
        if (CustomWardrobe.config.enabled) inCustomWardrobe = matched
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) = handleInventoryUpdated(event)

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) = handleInventoryClose()

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) = handleDebugDataCollect(event)
}
