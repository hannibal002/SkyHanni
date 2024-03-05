package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.init.Blocks
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BlockBaker {

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return

        val lookingAt = event.pos.offset(event.face).toLorenzVec()
        val equipped = InventoryUtils.getItemInHand() ?: return

        if (equipped.displayName.contains("Baker") && lookingAt.getBlockStateAt().block == Blocks.air) {

        }
    }
}
