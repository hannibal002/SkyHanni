package at.hannibal2.hanni.events.render.gui

import at.hannibal2.hanni.api.event.RenderingHanniEvent
import at.hannibal2.hanni.api.minecraftevents.RenderLayer
import at.hannibal2.hanni.utils.compat.DrawContext

class GameOverlayRenderPostEvent(context: DrawContext, val type: RenderLayer) : RenderingHanniEvent(context)
