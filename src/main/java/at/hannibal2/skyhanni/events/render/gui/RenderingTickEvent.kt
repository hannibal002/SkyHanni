package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext

class RenderingTickEvent(val context: DrawContext, val startPhase: Boolean) : SkyHanniEvent()
