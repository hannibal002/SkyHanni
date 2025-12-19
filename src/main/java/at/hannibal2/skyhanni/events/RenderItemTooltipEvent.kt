package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

class RenderItemTooltipEvent(context: GuiGraphics, val stack: ItemStack) : RenderingSkyHanniEvent(context)
