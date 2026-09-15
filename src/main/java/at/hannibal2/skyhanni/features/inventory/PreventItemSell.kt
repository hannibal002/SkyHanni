package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.item.ItemNotClickableEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid

@SkyHanniModule
object PreventItemSell {

    private val storage get() = ProfileStorageData.profileSpecific

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shpreventsell") {
            description = "Prevents items from selling to Auction House, to NPC's or to other players. Hold the item in hand to activate."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { onToggle() }
        }
    }

    private fun onToggle() {
        val stack = InventoryUtils.getItemInHand()
        if (stack == null) {
            ChatUtils.userError("Hold an item in hand!")
            return
        }

        val uuid = stack.getItemUuid()
        if (uuid == null) {
            ChatUtils.userError("Can only lock in items that have a uuid!")
            return
        }

        val notSellableItems = storage?.notSellableItems ?: error("storage is null")
        val name = stack.hoverName.string
        if (uuid !in notSellableItems) {
            notSellableItems.add(uuid)
            ChatUtils.chat("$name is §anow §eprotected from selling.")
        } else {
            notSellableItems.remove(uuid)
            ChatUtils.chat("$name is §cno longer §eprotected from selling.")
        }
    }

    private fun shouldPreventSell(stack: SafeItemStack): Boolean = stack.getItemUuid()?.let { uuid ->
        ProfileStorageData.profileSpecific?.notSellableItems?.let { list ->
            uuid in list
        }
    } ?: false

    @HandleEvent
    private fun onItemNotClickable(event: ItemNotClickableEvent) {
        val chestName = event.chestName
        val stack = event.stack
        if (!inASellerInventory(chestName, stack)) return
        if (!shouldPreventSell(stack)) return

        event.hideReasons += listOf(
            "You prevented the selling of this item!",
            "Disable it by holding the item in the hand",
            "and type §e/shpreventsell§e!",
        )
        event.allowBypass = false
    }

    private fun inASellerInventory(chestName: String, stack: SafeItemStack): Boolean =
        HideNotClickableItemsFeature.isAuctionHouse(chestName) ||
            inNpcSellInventory(stack) ||
            HideNotClickableItemsFeature.isTradeMenu(chestName)

    // Rift and Visitors are excluded the same way as in HideNotClickableItemsFeature.hideNpcSell
    private fun inNpcSellInventory(stack: SafeItemStack): Boolean =
        !RiftApi.inRift() && ShiftClickNpcSell.inInventory && !VisitorApi.inInventory &&
            HideNotClickableItemsFeature.npcSellable(stack)
}
