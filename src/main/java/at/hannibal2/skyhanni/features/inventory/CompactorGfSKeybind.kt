package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CompactorCraftApi
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.compat.slotUnderCursor
import net.minecraft.world.entity.player.Inventory

@SkyHanniModule
object CompactorGfSKeybind {

    private val config get() = SkyHanniMod.feature.inventory.gfs

    @HandleEvent(onlyOnSkyblock = true)
    private fun onKeyDown(event: KeyDownEvent) {
        if (event.keyCode != config.compactorKeybind) return
        val slot = slotUnderCursor() ?: return
        if (slot.container !is Inventory) return

        val internalName = slot.item.getInternalNameOrNull() ?: return
        grabMissing(internalName)
    }

    private fun grabMissing(internalName: NeuInternalName) {
        // Ignore silently, the previous request for this item is still on its way.
        if (GetFromSackApi.isQueued(internalName) || GetFromSackApi.wasRecentlySent(internalName)) return
        val itemName = internalName.repoItemName

        when (val state = CompactorCraftApi.getCraftState(internalName)) {
            NotLoaded -> ChatUtils.userError("Recipe data is not loaded yet.")
            NoCraft -> ChatUtils.userError("$itemName §ccannot be crafted into another item.")
            is Ambiguous -> ChatUtils.userError("$itemName §chas more than one craft at the same amount.")
            is Enough -> ChatUtils.userError("You already have enough $itemName §cfor ${state.upgrade.result.repoItemName}§c.")
            is Missing -> GetFromSackApi.getFromSack(internalName, state.amount)
        }
    }
}
