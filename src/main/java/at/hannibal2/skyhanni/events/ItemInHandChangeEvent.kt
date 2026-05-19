package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import net.minecraft.world.item.ItemStack

@PrimaryFunction("onItemInHandChange")
data class ItemInHandChangeEvent(
    val oldSlot: Int,
    val oldStack: ItemStack,
    val newSlot: Int,
    val newStack: ItemStack,
) : SkyHanniEvent() {
    val oldItem = oldStack.getInternalName()
    val newItem = newStack.getInternalName()
}
