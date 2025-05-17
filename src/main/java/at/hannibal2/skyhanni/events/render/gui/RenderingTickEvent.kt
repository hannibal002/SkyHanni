package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext

class RenderingTickEvent(context: DrawContext, val startPhase: Boolean) : RenderingSkyHanniEvent(context)
