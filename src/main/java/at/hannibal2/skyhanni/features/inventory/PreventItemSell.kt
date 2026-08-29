package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object PreventItemSell {

    private val storage get() = ProfileStorageData.profileSpecific

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shpreventsell") {
            description = "Prevents items from selling to Auction House, NPC's or other players. Hold the item in hand to protect."
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
        val name = stack.displayName.string
        if (uuid !in notSellableItems) {
            notSellableItems.add(uuid)
            ChatUtils.chat("$name is §anow §eprotected from selling.")
        } else {
            notSellableItems.remove(uuid)
            ChatUtils.chat("$name is §cno longer §eprotected from selling.")
        }
    }

    fun shouldPreventSell(stack: ItemStack): Boolean = stack.getItemUuid()?.let { uuid ->
        ProfileStorageData.profileSpecific?.notSellableItems?.let { list ->
            uuid in list
        }
    } ?: false
}
