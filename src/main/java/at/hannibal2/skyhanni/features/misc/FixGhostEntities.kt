package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * This feature fixes ghost entities sent by hypixel that are not properly deleted in the correct order.
 * This included Diana, Dungeon and Crimson Isle mobs and nametags.
 */
object FixGhostEntities {

    private val config get() = SkyHanniMod.feature.misc

    private var recentlyRemovedEntities = ArrayDeque<Int>()
    private var recentlySpawnedEntities = ArrayDeque<Int>()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        recentlyRemovedEntities = ArrayDeque()
        recentlySpawnedEntities = ArrayDeque()
    }

    @SubscribeEvent
    fun onReceiveCurrentShield(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet

        if (packet is S0CPacketSpawnPlayer) {
            if (packet.entityID in recentlyRemovedEntities) {
                event.cancel()
            }
            recentlySpawnedEntities.addLast(packet.entityID)
        } else if (packet is S0FPacketSpawnMob) {
            if (packet.entityID in recentlyRemovedEntities) {
                event.cancel()
            }
            recentlySpawnedEntities.addLast(packet.entityID)
        } else if (packet is S13PacketDestroyEntities) {
            for (entityID in packet.entityIDs) {
                // ingore entities that got properly spawned and then removed
                if (entityID !in recentlySpawnedEntities) {
                    recentlyRemovedEntities.addLast(entityID)
                    if (recentlyRemovedEntities.size == 10) {
                        recentlyRemovedEntities.removeFirst()
                    }
                }
            }
        }
    }

    fun isEnabled() = LorenzUtils.connectedToHypixel && config.fixGhostEntities
}
