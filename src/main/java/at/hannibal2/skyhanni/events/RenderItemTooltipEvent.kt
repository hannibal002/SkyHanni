package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

@PrimaryFunction("onRenderItemTooltip")
class RenderItemTooltipEvent(context: GuiGraphics, val stack: ItemStack) : RenderingSkyHanniEvent(context)
