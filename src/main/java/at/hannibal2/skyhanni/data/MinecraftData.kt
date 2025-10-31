package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ItemInHandChangeEvent
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.minecraft.ServerTickEvent
import at.hannibal2.hanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuInternalName
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S2APacketParticles
//#if MC < 1.21
import net.minecraft.network.play.server.S32PacketConfirmTransaction
//#else
//$$ import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket
//#endif

@HanniModule
object MinecraftData {

    @HandleEvent(receiveCancelled = true)
    fun onPacket(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is S29PacketSoundEffect -> {
                if (PlaySoundEvent(
                        //#if MC < 1.21
                        packet.soundName,
                        //#else
                        //$$ packet.sound.value().id.toString().removePrefix("minecraft:"),
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

            //#if MC < 1.21
            is S32PacketConfirmTransaction -> {
                if (packet.actionNumber > 0) return
                //#else
                //$$ is CommonPingS2CPacket -> {
                //$$ if (lastPingParameter == packet.parameter) return
                //$$ lastPingParameter = packet.parameter
                //#endif

                totalServerTicks++
                ServerTickEvent.post()
            }
        }
    }

    //#if MC > 1.21
    //$$ private var lastPingParameter = 0
    //#endif

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
