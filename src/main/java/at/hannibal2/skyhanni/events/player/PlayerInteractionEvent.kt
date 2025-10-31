package at.hannibal2.hanni.events.player

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

class PlayerInteractionEvent(val action: ClickAction, val pos: BlockPos?, val face: EnumFacing?) : HanniEvent()

enum class ClickAction {
    RIGHT_CLICK_AIR,
    RIGHT_CLICK_BLOCK,
    LEFT_CLICK_BLOCK;

    companion object {
        //#if MC < 1.21
        fun fromForge(old: net.minecraftforge.event.entity.player.PlayerInteractEvent.Action): ClickAction {
            return ClickAction.entries[old.ordinal]
        }
        //#endif

    }
}
