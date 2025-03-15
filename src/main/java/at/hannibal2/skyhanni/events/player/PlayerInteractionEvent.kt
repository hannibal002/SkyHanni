package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraftforge.event.entity.player.PlayerInteractEvent

class PlayerInteractionEvent(val action: PlayerInteractEvent.Action, val pos: BlockPos?, val face: EnumFacing?) : SkyHanniEvent()
