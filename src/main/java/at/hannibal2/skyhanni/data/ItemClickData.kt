package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.getUsedItem
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
//#if MC > 1.21
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand
//#endif

@SkyHanniModule
object ItemClickData {

    @HandleEvent
    fun onItemClickSend(event: PacketSentEvent) {
        val packet = event.packet
        val cancelled = when {
            packet is PlayerInteractBlockC2SPacket -> {
                val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.RIGHT_CLICK).post()
                //#if MC < 1.16
                //$$ val didntMiss = packet.placedBlockDirection != 255
                //#else
                val didntMiss = !packet.blockHitResult.missed
                //#endif
                if (didntMiss) {
                    //#if MC < 1.16
                    //$$ val position = packet.position.toLorenzVec()
                    //#else
                    val position = packet.blockHitResult.blockPos.toLorenzVec()
                    //#endif
                    BlockClickEvent(ClickType.RIGHT_CLICK, position, packet.getUsedItem()).post() || clickCancelled
                } else {
                    clickCancelled
                }
            }

            //#if MC > 1.21
            packet is PlayerInteractItemC2SPacket -> {
                ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.RIGHT_CLICK).post()
            }
            //#endif

            //#if MC < 1.21
            //$$ // MixinClientPlayerInteractionManager posts this on 1.21
            //$$ packet is PlayerActionC2SPacket && packet.action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK -> {
            //$$     val position = packet.pos.toLorenzVec()
            //$$     val blockClickCancelled =
            //$$         BlockClickEvent(ClickType.LEFT_CLICK, position, InventoryUtils.getItemInHand()).post()
            //$$     ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).also {
            //$$         if (blockClickCancelled) it.cancel()
            //$$     }.post()
            //$$ }
            //#endif

            packet is HandSwingC2SPacket -> {
                ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).post()
            }

            packet is PlayerInteractEntityC2SPacket -> {
                val clickType = when (packet.type.getType()) {
                    PlayerInteractEntityC2SPacket.InteractType.INTERACT -> ClickType.RIGHT_CLICK
                    PlayerInteractEntityC2SPacket.InteractType.ATTACK -> ClickType.LEFT_CLICK
                    PlayerInteractEntityC2SPacket.InteractType.INTERACT_AT -> ClickType.RIGHT_CLICK
                    else -> return
                }
                //#if MC < 1.21
                //$$ val clickedEntity = packet.getEntity(MinecraftCompat.localWorld) ?: return
                //#else
                if (packet.type is PlayerInteractEntityC2SPacket.InteractHandler) {
                    if ((packet.type as PlayerInteractEntityC2SPacket.InteractHandler).hand == Hand.OFF_HAND) return
                }
                val world = MinecraftCompat.localPlayer.world
                val clickedEntity = world.getEntityById(packet.entityId) ?: return
                //#endif
                EntityClickEvent(clickType, packet.type.getType(), clickedEntity, InventoryUtils.getItemInHand()).post()
            }

            else -> {
                return
            }
        }

        if (cancelled) {
            event.cancel()
        }
    }
}
