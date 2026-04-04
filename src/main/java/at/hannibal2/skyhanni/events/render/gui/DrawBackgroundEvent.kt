package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.client.gui.GuiGraphics

@PrimaryFunction("onBackgroundDraw")
class DrawBackgroundEvent(context: GuiGraphics) : RenderingSkyHanniEvent(context)
