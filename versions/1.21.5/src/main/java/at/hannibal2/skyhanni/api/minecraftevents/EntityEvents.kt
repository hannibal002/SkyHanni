package at.hannibal2.hanni.api.minecraftevents

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.mob.MobData
import at.hannibal2.hanni.data.mob.MobDetection
import at.hannibal2.hanni.events.entity.EntityHurtEvent
import at.hannibal2.hanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.compat.DamageSourceCompat
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket

@HanniModule
object EntityEvents {

    @HandleEvent
    fun onPacketReceived(event: PacketReceivedEvent) {
        val packet = event.packet as? DamageTiltS2CPacket ?: return

        val entity = MinecraftCompat.localWorld.getEntityById(packet.id()) ?: return
        EntityHurtEvent(entity, DamageSourceCompat.generic, 0.0f).post()

        val skyblockMob = MobData.entityToMob[entity] ?: return
        MobDetection.postMobHurtEvent(skyblockMob, DamageSourceCompat.generic, 0.0f)
    }
}
