package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.mob.MobDetection
import at.hannibal2.skyhanni.events.entity.EntityHurtEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.DamageSourceCompat
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket

@SkyHanniModule
object EntityEvents {

    @HandleEvent
    fun onPacketReceived(event: PacketReceivedEvent) {
        val packet = event.packet as? ClientboundHurtAnimationPacket ?: return

        val entity = MinecraftCompat.localWorld.getEntity(packet.id()) ?: return
        EntityHurtEvent(entity, DamageSourceCompat.generic, 0.0f).post()

        val skyblockMob = MobData.entityToMob[entity] ?: return
        MobDetection.postMobHurtEvent(skyblockMob, DamageSourceCompat.generic, 0.0f)
    }
}
