package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EntityData {

    @SubscribeEvent
    fun onHealthUpdatePacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet !is S1CPacketEntityMetadata) return

        if (packet == null) {
            LorenzUtils.debug("packet is null in CorruptedMobHigh   light!")
            return
        }

        val watchableObjects = packet.func_149376_c() ?: return
        for (watchableObject in watchableObjects) {
            if (watchableObject.dataValueId != 6) continue

            val theWorld = Minecraft.getMinecraft().theWorld
            if (theWorld == null) {
                LorenzUtils.debug("theWorld is null in CorruptedMobHighlight!")
                continue
            }
            val entityId = packet.entityId
            if (entityId == null) {
                LorenzUtils.debug("entityId is null in CorruptedMobHighlight!")
                continue
            }

            val entity = theWorld.getEntityByID(entityId) ?: continue
            if (entity !is EntityLivingBase) continue

            val health = watchableObject.`object` as Float
            EntityHealthUpdateEvent(entity, health).postAndCatch()
            return
        }
    }
}