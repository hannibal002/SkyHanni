package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.RenderItemTooltipEvent
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack

fun renderToolTip(context: DrawContext, stack: ItemStack) {
    RenderItemTooltipEvent(context, stack).post()
}
