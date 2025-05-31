package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S2APacketParticles
//#if TODO
import net.minecraft.network.play.server.S32PacketConfirmTransaction
//#endif
//#if MC > 1.21
//$$ import net.minecraft.registry.Registries
//#endif

// todo 1.21 impl needed
@SkyHanniModule
object MinecraftData {

    @HandleEvent(receiveCancelled = true)
    fun onPacket(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is S29PacketSoundEffect -> {
                if (PlaySoundEvent(
                        //#if MC < 1.21
                        packet.soundName,
                        //#else
                        //$$ packet.sound.value().id.toString(),
                        //#endif
                        LorenzVec(packet.x, packet.y, packet.z), packet.pitch, packet.volume,
                    ).post()
                ) {
                    event.cancel()
                }
            }

            is S2APacketParticles -> {
                if (ReceiveParticleEvent(
                        //#if MC < 1.21
                        packet.particleType,
                        //#else
                        //$$ packet.parameters.type,
                        //#endif
                        LorenzVec(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate),
                        packet.particleCount,
                        packet.particleSpeed,
                        LorenzVec(packet.xOffset, packet.yOffset, packet.zOffset),
                        packet.isLongDistance,
                        //#if MC < 1.21
                        packet.particleArgs,
                        //#endif
                    ).post()
                ) {
                    event.cancel()
                }
            }

            //#if TODO
            is S32PacketConfirmTransaction -> {
                if (packet.actionNumber > 0) return

                totalServerTicks++
                ServerTickEvent.post()
            }
            //#endif
        }
    }

    var totalServerTicks: Long = 0L
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        val hand = InventoryUtils.getItemInHand()
        val newItem = hand?.getInternalName() ?: NeuInternalName.NONE
        val oldItem = InventoryUtils.itemInHandId
        if (newItem != oldItem) {
            if (newItem != NeuInternalName.NONE) InventoryUtils.recentItemsInHand.add(newItem)
            InventoryUtils.itemInHandId = newItem
            InventoryUtils.latestItemInHand = hand
            ItemInHandChangeEvent(newItem, oldItem).post()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        InventoryUtils.itemInHandId = NeuInternalName.NONE
        InventoryUtils.recentItemsInHand.clear()
    }
}
