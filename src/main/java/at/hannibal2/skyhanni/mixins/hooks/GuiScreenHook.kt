package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.RenderItemTooltipEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

fun renderToolTip(context: DrawContext, stack: ItemStack) {
    RenderItemTooltipEvent(context, stack).post()
}
