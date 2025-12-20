package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class PlayerInteractionEvent(val action: ClickAction, val pos: BlockPos?, val face: Direction?) : SkyHanniEvent()

enum class ClickAction {
    RIGHT_CLICK_AIR,
    RIGHT_CLICK_BLOCK,
    LEFT_CLICK_BLOCK,
}
