package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
//? if < 26.1 {
import net.minecraft.client.gui.GuiGraphics
//?} else
//import net.minecraft.client.gui.GuiGraphicsExtractor

@PrimaryFunction("onRenderingTick")
class RenderingTickEvent(
    //? if < 26.1 {
    context: GuiGraphics,
    //?} else
    //context: GuiGraphicsExtractor,
    val startPhase: Boolean,
) : RenderingSkyHanniEvent(context)
