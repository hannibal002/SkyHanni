package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent

class ItemClickData {

    @SubscribeEvent
    fun onItemClickSend(event: PacketEvent.SendEvent) {
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement) {
            if (packet.placedBlockDirection != 255) {
                val position = packet.position.toLorenzVec()
                event.isCanceled = BlockClickEvent(ClickType.RIGHT_CLICK, position, packet.stack).postAndCatch()
            } else {
                event.isCanceled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.RIGHT_CLICK).postAndCatch()
            }
        }
        if (packet is C07PacketPlayerDigging && packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
            val position = packet.position.toLorenzVec()
            val blockClickCancelled = BlockClickEvent(ClickType.LEFT_CLICK, position, InventoryUtils.getItemInHand()).postAndCatch()
            event.isCanceled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).also { it.isCanceled = blockClickCancelled }.postAndCatch()
        }
        if (packet is C0APacketAnimation) {
            event.isCanceled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).postAndCatch()
        }
    }

    @SubscribeEvent
    fun onEntityClick(event: InputEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val minecraft = Minecraft.getMinecraft()

        val attackKey = minecraft.gameSettings.keyBindAttack
        val useKey = minecraft.gameSettings.keyBindUseItem

        val clickType = when {
            attackKey.isKeyDown -> ClickType.LEFT_CLICK
            useKey.isKeyDown -> ClickType.RIGHT_CLICK
            else -> return
        }

        val clickedEntity = minecraft.pointedEntity
        if (minecraft.thePlayer == null) return
        if (clickedEntity == null) return

        EntityClickEvent(clickType, clickedEntity, InventoryUtils.getItemInHand()).postAndCatch()
    }
}
