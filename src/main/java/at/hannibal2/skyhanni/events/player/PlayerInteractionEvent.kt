package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

class PlayerInteractionEvent(val action: Action, val pos: BlockPos?, val face: EnumFacing?) : SkyHanniEvent()

enum class Action {
    RIGHT_CLICK_AIR,
    RIGHT_CLICK_BLOCK,
    LEFT_CLICK_BLOCK;

    companion object {
        //#if MC < 1.21
        fun fromForge(old: net.minecraftforge.event.entity.player.PlayerInteractEvent.Action): Action {
            return Action.entries[old.ordinal]
        }
        //#endif

    }
}
