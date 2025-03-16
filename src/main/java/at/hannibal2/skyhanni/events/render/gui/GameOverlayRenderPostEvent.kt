package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent

class GameOverlayRenderPostEvent(val type: RenderGameOverlayEvent.ElementType) : SkyHanniEvent()
