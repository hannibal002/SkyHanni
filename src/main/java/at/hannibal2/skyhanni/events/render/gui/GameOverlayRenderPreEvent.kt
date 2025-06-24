package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.utils.compat.DrawContext

class GameOverlayRenderPreEvent(context: DrawContext, val type: RenderLayer) :
    RenderingSkyHanniEvent(context), SkyHanniEvent.Cancellable
