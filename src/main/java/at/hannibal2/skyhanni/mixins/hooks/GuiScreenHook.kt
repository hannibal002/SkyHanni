package at.hannibal2.hanni.mixins.hooks

import at.hannibal2.hanni.events.RenderItemTooltipEvent
import at.hannibal2.hanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

fun renderToolTip(context: DrawContext, stack: ItemStack) {
    RenderItemTooltipEvent(context, stack).post()
}
