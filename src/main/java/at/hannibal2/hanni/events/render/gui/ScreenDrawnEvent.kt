package at.hannibal2.hanni.events.render.gui

import at.hannibal2.hanni.api.event.RenderingHanniEvent
import at.hannibal2.hanni.utils.compat.DrawContext
import net.minecraft.client.gui.GuiScreen

class ScreenDrawnEvent(context: DrawContext, val gui: GuiScreen?) : RenderingHanniEvent(context)
