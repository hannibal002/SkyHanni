package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.client.gui.GuiScreen

class ScreenDrawnEvent(context: DrawContext, val gui: GuiScreen?) : RenderingSkyHanniEvent(context)
