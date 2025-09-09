package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class PlayerInteractionEvent(val action: ClickAction, val pos: BlockPos?, val face: Direction?) : SkyHanniEvent()

enum class ClickAction {
    RIGHT_CLICK_AIR,
    RIGHT_CLICK_BLOCK,
    LEFT_CLICK_BLOCK;

    companion object {
        //#if MC < 1.21
        //$$ fun fromForge(old: net.minecraftforge.event.entity.player.PlayerInteractEvent.Action): ClickAction {
        //$$     return ClickAction.entries[old.ordinal]
        //$$ }
        //#endif

    }
}
