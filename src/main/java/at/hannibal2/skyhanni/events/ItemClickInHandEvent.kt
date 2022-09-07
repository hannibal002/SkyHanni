package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

class ItemClickInHandEvent(val clickType: ClickType, val itemInHand: ItemStack?): LorenzEvent() {

    enum class ClickType {
        LEFT_CLICK, RIGHT_CLICK
    }
}