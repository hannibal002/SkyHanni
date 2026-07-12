package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen

@Thread(RENDER)
class ScreenDrawnEvent(context: GuiGraphicsExtractor, val gui: Screen?) : RenderingSkyHanniEvent(context)
