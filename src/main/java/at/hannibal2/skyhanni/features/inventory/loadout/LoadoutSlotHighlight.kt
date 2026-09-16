package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object LoadoutSlotHighlight {
    val config get() = SkyHanniMod.feature.inventory.customLoadout.highlighting

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        LoadoutApi.slots
            .filter { it.isInCurrentPage() }
            .forEach { loadoutSlot ->
                val slot = event.container.getSlot(loadoutSlot.inventorySlot)
                when {
                    config.currentlyEquipped && LoadoutApi.currentSlot == loadoutSlot.id -> slot.highlight(config.equippedColor)
                    config.favorites && loadoutSlot.favorite -> slot.highlight(config.favoriteColor)
                }
            }
    }

    fun isEnabled() = config.enabled && LoadoutApi.inLoadouts()
}
