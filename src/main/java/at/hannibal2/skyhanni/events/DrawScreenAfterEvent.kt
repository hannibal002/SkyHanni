package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphicsExtractor

class DrawScreenAfterEvent(
    context: GuiGraphicsExtractor,
    val mouseX: Int,
    val mouseY: Int,
) : RenderingSkyHanniEvent(context)
