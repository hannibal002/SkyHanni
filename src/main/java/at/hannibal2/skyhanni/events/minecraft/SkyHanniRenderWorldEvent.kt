package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.WorldRenderContext

class SkyHanniRenderWorldEvent(val context: WorldRenderContext, val partialTicks: Float) : SkyHanniEvent()
