package at.hannibal2.skyhanni.api.event

import net.minecraft.client.gui.GuiGraphics

abstract class RenderingSkyHanniEvent(
    override val context: GuiGraphics,
) : SkyHanniEvent(), SkyHanniEvent.Rendering
