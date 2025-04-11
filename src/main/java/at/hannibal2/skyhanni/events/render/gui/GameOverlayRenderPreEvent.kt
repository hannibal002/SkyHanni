package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraftforge.client.event.RenderGameOverlayEvent

class GameOverlayRenderPreEvent(val context: DrawContext, val type: RenderGameOverlayEvent.ElementType) : CancellableSkyHanniEvent()
