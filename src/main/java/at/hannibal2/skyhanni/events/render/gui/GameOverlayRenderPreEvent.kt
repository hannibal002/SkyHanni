package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.client.gui.GuiGraphicsExtractor

@Thread(RENDER)
class GameOverlayRenderPreEvent(context: GuiGraphicsExtractor, val type: RenderLayer) :
    RenderingSkyHanniEvent(context), SkyHanniEvent.Cancellable
