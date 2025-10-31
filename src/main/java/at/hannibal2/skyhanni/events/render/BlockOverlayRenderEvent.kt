package at.hannibal2.hanni.events.render

import at.hannibal2.hanni.api.event.CancellableHanniEvent

class BlockOverlayRenderEvent(val overlayType: OverlayType) : CancellableHanniEvent()

enum class OverlayType {
    FIRE,
    BLOCK,
    WATER;

    companion object {
        //#if FORGE
        fun fromForge(old: net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType): OverlayType {
            return entries[old.ordinal]
        }
        //#endif
    }
}
