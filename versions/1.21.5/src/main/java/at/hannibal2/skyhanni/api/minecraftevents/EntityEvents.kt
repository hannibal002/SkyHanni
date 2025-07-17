package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.mob.MobDetection
import at.hannibal2.skyhanni.events.entity.EntityHurtEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket

@SkyHanniModule
object EntityEvents {

    @HandleEvent
    fun onPacketReceived(event: PacketReceivedEvent) {
        val packet = event.packet as? DamageTiltS2CPacket ?: return

        val world = MinecraftClient.getInstance().world ?: return
        val entity = world.getEntityById(packet.id()) ?: return
        EntityHurtEvent(entity, world.damageSources.generic(), 0.0f).post()

        val skyblockMob = MobData.entityToMob[entity] ?: return
        MobDetection.postMobHurtEvent(skyblockMob, world.damageSources.generic(), 0.0f)
    }
}
