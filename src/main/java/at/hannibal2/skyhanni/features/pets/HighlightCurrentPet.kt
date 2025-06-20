package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.api.pet.PetStorageApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo

@SkyHanniModule
object HighlightCurrentPet {

    private val config get() = SkyHanniMod.feature.misc.pets.highlightInMenu

    private var inInventory = false
    private var highlightSlot: Int? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.enabled || !inInventory) return
        val highlightSlot = highlightSlot ?: return
        val slotToHighlight = InventoryUtils.getItemsInOpenChest().firstOrNull {
            it.slotNumber == highlightSlot
        } ?: return
        slotToHighlight.highlight(config.color)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = PetStorageApi.mainPetMenuNamePattern.matches(event.inventoryName)
        if (!inInventory) return
        val currentPetUuid = CurrentPetApi.currentPet?.uuid ?: return
        highlightSlot = event.inventoryItems.entries.firstOrNull {
            it.value.getPetInfo()?.uniqueId == currentPetUuid
        }?.key
    }
}
