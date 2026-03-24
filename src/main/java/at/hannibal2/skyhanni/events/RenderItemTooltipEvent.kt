package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.client.gui.GuiGraphics

@PrimaryFunction("onRenderItemTooltip")
class RenderItemTooltipEvent(context: GuiGraphics, val stack: SafeItemStack) : RenderingSkyHanniEvent(context)
