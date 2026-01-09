package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphics

class RenderingTickEvent(context: GuiGraphics, val startPhase: Boolean) : RenderingSkyHanniEvent(context)
