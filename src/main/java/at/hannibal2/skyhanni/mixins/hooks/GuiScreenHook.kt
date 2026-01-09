package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.RenderItemTooltipEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

fun renderToolTip(context: GuiGraphics, stack: ItemStack) {
    RenderItemTooltipEvent(context, stack).post()
}
