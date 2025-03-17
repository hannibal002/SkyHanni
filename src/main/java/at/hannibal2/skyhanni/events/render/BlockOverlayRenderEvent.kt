package at.hannibal2.skyhanni.events.render

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraftforge.client.event.RenderBlockOverlayEvent

class BlockOverlayRenderEvent(val overlayType: RenderBlockOverlayEvent.OverlayType) : CancellableSkyHanniEvent()
