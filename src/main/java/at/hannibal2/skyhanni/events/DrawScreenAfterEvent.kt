package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphics

class DrawScreenAfterEvent(context: GuiGraphics, val mouseX: Int, val mouseY: Int) : RenderingSkyHanniEvent(context)
