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
import net.minecraft.network.play.server.S32PacketConfirmTransaction

@SkyHanniModule
object MinecraftData {

    @HandleEvent(receiveCancelled = true)
    fun onPacket(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is S29PacketSoundEffect -> {
                if (PlaySoundEvent(packet.soundName, LorenzVec(packet.x, packet.y, packet.z), packet.pitch, packet.volume).post()) {
                    event.cancel()
                }
            }

            is S2APacketParticles -> {
                if (ReceiveParticleEvent(
                        packet.particleType,
                        LorenzVec(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate),
                        packet.particleCount,
                        packet.particleSpeed,
                        LorenzVec(packet.xOffset, packet.yOffset, packet.zOffset),
                        packet.isLongDistance,
                        packet.particleArgs,
                    ).post()
                ) {
                    event.cancel()
                }
            }

            is S32PacketConfirmTransaction -> {
                totalServerTicks++
                ServerTickEvent.post()
            }
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

            InventoryUtils.recentItemsInHand.keys.removeIf { it + 30_000 > System.currentTimeMillis() }
            if (newItem != NeuInternalName.NONE) {
                InventoryUtils.recentItemsInHand[System.currentTimeMillis()] = newItem
            }
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
