package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent

class GameOverlayRenderPreEvent(val type: RenderGameOverlayEvent.ElementType) : CancellableSkyHanniEvent()
