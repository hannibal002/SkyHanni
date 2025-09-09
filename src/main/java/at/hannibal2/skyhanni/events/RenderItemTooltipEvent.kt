package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack

class RenderItemTooltipEvent(context: DrawContext, val stack: ItemStack) : RenderingSkyHanniEvent(context)
