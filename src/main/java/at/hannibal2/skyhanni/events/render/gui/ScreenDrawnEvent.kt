package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen

class ScreenDrawnEvent(context: DrawContext, val gui: Screen?) : RenderingSkyHanniEvent(context)
