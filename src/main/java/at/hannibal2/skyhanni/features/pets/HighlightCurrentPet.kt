package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.api.pet.PetStorageApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo

@SkyHanniModule
object HighlightCurrentPet {

    private val config get() = SkyHanniMod.feature.misc.pets.highlightInMenu
    private var highlightSlot: Int? = null
    private var correctPetPage: Int? = null

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.enabled) return
        val currentPage = PetStorageApi.petMenuPageNumber(InventoryUtils.openInventoryName()) ?: return
        if (currentPage != correctPetPage) return
        val highlightSlot = highlightSlot ?: return
        event.container.slots[highlightSlot].highlight(config.color)
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.enabled) return
        val currentPage = PetStorageApi.petMenuPageNumber(event.inventoryName) ?: return
        val currentPet = CurrentPetApi.currentPet
        if (currentPet == null) {
            highlightSlot = null
            return
        }
        val petSlot = event.inventoryItems.entries.firstOrNull { it.value.getPetInfo()?.ownedUuid == currentPet.uuid }?.key
        if (petSlot == null) {
            correctPetPage = null
            highlightSlot = null
            return
        }
        correctPetPage = currentPage
        highlightSlot = petSlot
    }
}
