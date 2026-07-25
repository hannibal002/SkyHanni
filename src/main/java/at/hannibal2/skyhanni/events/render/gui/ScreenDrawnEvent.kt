package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen

class ScreenDrawnEvent(context: GuiGraphicsExtractor, val gui: Screen?) : RenderingSkyHanniEvent(context)
