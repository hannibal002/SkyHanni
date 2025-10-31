package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.RenderingHanniEvent
import at.hannibal2.hanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

class RenderItemTooltipEvent(context: DrawContext, val stack: ItemStack) : RenderingHanniEvent(context)
