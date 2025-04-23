package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraftforge.client.event.RenderGameOverlayEvent

class GameOverlayRenderPreEvent(context: DrawContext, val type: RenderGameOverlayEvent.ElementType) :
    RenderingSkyHanniEvent(context), SkyHanniEvent.Cancellable
