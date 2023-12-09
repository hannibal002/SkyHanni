package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object MinecraftData {

    @SubscribeEvent(receiveCanceled = true)
    fun onSoundPacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val packet = event.packet
        if (packet !is S29PacketSoundEffect) return

        if (PlaySoundEvent(
                packet.soundName,
                LorenzVec(packet.x, packet.y, packet.z),
                packet.pitch,
                packet.volume
            ).postAndCatch()
        ) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        LorenzWorldChangeEvent().postAndCatch()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onParticlePacketReceive(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val packet = event.packet
        if (packet !is S2APacketParticles) return

        if (ReceiveParticleEvent(
                packet.particleType!!,
                LorenzVec(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate),
                packet.particleCount,
                packet.particleSpeed,
                LorenzVec(packet.xOffset, packet.yOffset, packet.zOffset),
                packet.isLongDistance,
                packet.particleArgs,
            ).postAndCatch()
        ) {
            event.isCanceled = true
        }
    }

    var totalTicks = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        Minecraft.getMinecraft().thePlayer ?: return
        totalTicks++
        LorenzTickEvent(totalTicks).postAndCatch()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        DelayedRun.checkRuns()
        if (!LorenzUtils.inSkyBlock) return
        val hand = InventoryUtils.getItemInHand()
        val newItem = hand?.getInternalName() ?: NEUInternalName.NONE
        val oldItem = InventoryUtils.itemInHandId
        if (newItem != oldItem) {

            InventoryUtils.recentItemsInHand.keys.removeIf { it + 30_000 > System.currentTimeMillis() }
            if (newItem != NEUInternalName.NONE) {
                InventoryUtils.recentItemsInHand[System.currentTimeMillis()] = newItem
            }
            InventoryUtils.itemInHandId = newItem
            InventoryUtils.latestItemInHand = hand
            ItemInHandChangeEvent(newItem, oldItem).postAndCatch()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        InventoryUtils.itemInHandId = NEUInternalName.NONE
        InventoryUtils.recentItemsInHand.clear()
    }
}
