package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonHideTNT {

    private val tnt = mutableMapOf<EntityArmorStand, Long>()

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return

        val entity = event.entity
        if (entity is EntityArmorStand) {

            if (entity.name.startsWith("§9Superboom TNT")) {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.cleanName() == "Superboom TNT") {
                    event.isCanceled = true
                    tnt[entity] = System.currentTimeMillis()
                }
            }
        }
    }

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet
        if (packet is S2APacketParticles) {
            val packetLocation = LorenzVec(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate)
            for (armorStand in tnt.filter { it.value + 100 > System.currentTimeMillis() }.map { it.key }) {
                val distance = packetLocation.distance(armorStand.getLorenzVec())
                if (distance < 2) {
                    //only hiding white "sparkling" particles
                    if (packet.particleType.particleID == 3) {
                        event.isCanceled = true
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        tnt.clear()
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inDungeons && SkyHanniMod.feature.dungeon.hideTNT
    }
}