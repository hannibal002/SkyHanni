package at.hannibal2.skyhanni.api.event

import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class RenderingSkyHanniEvent(
    override val context: GuiGraphicsExtractor,
) : SkyHanniEvent(), SkyHanniEvent.Rendering
