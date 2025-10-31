package at.hannibal2.hanni.events.render.gui

import at.hannibal2.hanni.api.event.RenderingHanniEvent
import at.hannibal2.hanni.utils.compat.DrawContext

class RenderingTickEvent(context: DrawContext, val startPhase: Boolean) : RenderingHanniEvent(context)
