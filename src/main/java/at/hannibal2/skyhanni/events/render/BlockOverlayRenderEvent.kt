package at.hannibal2.skyhanni.events.render

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent

class BlockOverlayRenderEvent(val overlayType: OverlayType) : CancellableSkyHanniEvent()

enum class OverlayType {
    FIRE,
    BLOCK,
    WATER;
}
