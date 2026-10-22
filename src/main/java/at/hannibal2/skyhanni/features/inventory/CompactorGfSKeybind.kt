package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CompactorCraftApi
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.drawBorder
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot

@SkyHanniModule
object CompactorGfSKeybind {

    private val config get() = SkyHanniMod.feature.inventory.gfs

    private const val OVERLAY_OPACITY = 130
    private const val BORDER_OPACITY = 200

    // HideNotClickableItems clears the whole tooltip at LOWEST, so this has to run after it.
    private const val TOOLTIP_PRIORITY = HandleEvent.LOWEST + 1

    private fun isActive(): Boolean = config.compactorKeybind.isKeyHeld()

    private val Slot.isOwnInventory: Boolean get() = container is Inventory

    private fun isPending(internalName: NeuInternalName): Boolean =
        GetFromSackApi.isQueued(internalName) || GetFromSackApi.wasRecentlySent(internalName)

    @HandleEvent(onlyOnSkyblock = true)
    private fun onForegroundDrawn(event: GuiContainerEvent.ForegroundDrawnEvent) {
        if (!isActive()) return
        val amounts = countOwnInventory()

        for (slot in event.container.slots) {
            if (!slot.isOwnInventory) continue
            val internalName = slot.item.getInternalNameOrNull() ?: continue

            val state = CompactorCraftApi.getCraftState(internalName, amounts[internalName] ?: 0)
            // A pending request only matters for items that have a craft at all.
            if (state.hasCraft && isPending(internalName)) {
                slot.highlight(LorenzColor.YELLOW.addOpacity(OVERLAY_OPACITY))
                continue
            }

            if (state is Missing) {
                slot.drawBorder(LorenzColor.GREEN.addOpacity(BORDER_OPACITY))
            } else {
                slot.highlight(LorenzColor.DARK_GRAY.addOpacity(OVERLAY_OPACITY))
            }
        }
    }

    /** Counted once per frame, so that the state of every slot does not scan the inventory again. */
    private fun countOwnInventory(): Map<NeuInternalName, Int> = buildMap {
        for (stack in InventoryUtils.getItemsInOwnInventory()) {
            val internalName = stack.getInternalNameOrNull() ?: continue
            addOrPut(internalName, stack.count)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isActive()) return
        val slot = event.slot ?: return
        if (!slot.isOwnInventory) return
        // Swallow every click while the key is held, so nothing gets picked up by accident.
        event.cancel()
        if (!event.mouseType.isLeftClick()) return

        val internalName = slot.item.getInternalNameOrNull() ?: return
        grabMissing(internalName)
    }

    private fun grabMissing(internalName: NeuInternalName) {
        // Ignore silently, the previous request for this item is still on its way.
        if (isPending(internalName)) return

        val state = CompactorCraftApi.getCraftState(internalName)
        if (state == NotLoaded) {
            ChatUtils.userError("Recipe data is not loaded yet.")
            return
        }
        val missing = state as? Missing ?: return
        GetFromSackApi.getFromSack(internalName, missing.amount)
    }

    @HandleEvent(onlyOnSkyblock = true, priority = TOOLTIP_PRIORITY)
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!isActive()) return
        val slot = event.slot ?: return
        if (!slot.isOwnInventory) return
        val internalName = event.itemStack.getInternalNameOrNull() ?: return

        val state = CompactorCraftApi.getCraftState(internalName)
        val pending = state.hasCraft && isPending(internalName)
        val itemName = event.toolTip.firstOrNull() ?: return

        event.toolTip.clear()
        // Only a clickable item keeps its rarity color.
        if (!pending && state is Missing) {
            event.toolTip.add(itemName)
        } else {
            event.toolTip.add("§7${itemName.string}".asComponent())
        }

        event.toolTip.add("".asComponent())
        event.toolTip.add(statusLine(pending, state).asComponent())
    }

    private fun statusLine(pending: Boolean, state: CompactorCraftApi.CraftState): String {
        if (pending) return "§7Already requested"
        return when (state) {
            is Missing -> "§eClick to grab §ax${state.amount} §emore for ${state.upgrade.result.repoItemName}"
            is Enough -> "§7Already enough for ${state.upgrade.result.repoItemName}"
            is Ambiguous -> "§7More than one craft at the same amount"
            NoCraft -> "§7Cannot be crafted into another item"
            NotLoaded -> "§7Recipe data is still loading"
        }
    }
}
