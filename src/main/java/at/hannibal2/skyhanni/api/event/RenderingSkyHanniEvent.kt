package at.hannibal2.skyhanni.api.event

//? if < 26.1 {
import net.minecraft.client.gui.GuiGraphics
//?} else
//import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class RenderingSkyHanniEvent(
    //? if < 26.1 {
    override val context: GuiGraphics
    //?} else
    //override val context: GuiGraphicsExtractor
) : SkyHanniEvent(), SkyHanniEvent.Rendering
