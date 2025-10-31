package at.hannibal2.hanni.features.pets

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.pet.CurrentPetApi
import at.hannibal2.hanni.api.pet.PetStorageApi
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getPetInfo

@HanniModule
object HighlightCurrentPet {

    private val config get() = HanniMod.feature.misc.pets.highlightInMenu

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
