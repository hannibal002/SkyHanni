package at.hannibal2.hanni.events.minecraft

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.compat.WorldRenderContext

class HanniRenderWorldEvent(val context: WorldRenderContext, val partialTicks: Float) : HanniEvent()
