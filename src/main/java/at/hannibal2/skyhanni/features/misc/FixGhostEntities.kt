package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.compat.getAllEquipment
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import java.util.function.IntConsumer

/**
 * This feature fixes ghost entities sent by hypixel that are not properly deleted in the correct order.
 * This included Diana, Dungeon and Crimson Isle mobs and nametags.
 */
@SkyHanniModule
object FixGhostEntities {

    private val config get() = SkyHanniMod.feature.misc

    private var recentlyRemovedEntities = ArrayDeque<Int>()
    private var recentlySpawnedEntities = ArrayDeque<Int>()
    private val hiddenEntityIds = mutableListOf<Int>()

    @HandleEvent
    fun onWorldChange() {
        recentlyRemovedEntities = ArrayDeque()
        recentlySpawnedEntities = ArrayDeque()
        hiddenEntityIds.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        if (!config.fixGhostEntities) return
        // Disable in Kuudra for now - causes players to randomly disappear in supply phase
        // TODO: Remove once fixed

        // Disabled on modern versions as the detection is not fully correct leading to incorrect hiding of entities
        // TODO fix this
        if (KuudraApi.inKuudra || true) return

        when (val packet = event.packet) {
            is ClientboundAddEntityPacket -> {
                if (packet.id in recentlyRemovedEntities) {
                    hiddenEntityIds.add(packet.id)
                }
                recentlySpawnedEntities.addLast(packet.id)
            }

            is ClientboundRemoveEntitiesPacket -> packet.entityIds.forEach(
                IntConsumer { entityID ->
                    if (entityID !in recentlySpawnedEntities) {
                        recentlyRemovedEntities.addLast(entityID)
                        if (recentlyRemovedEntities.size == 10) recentlyRemovedEntities.removeFirst()
                    }
                    hiddenEntityIds.remove(entityID)
                }
            )
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (config.hideTemporaryArmorStands) {
            (event.entity as? ArmorStand)?.let { stand ->
                if (stand.tickCount < 10 && stand.isDefaultValue() && stand.getAllEquipment().all { it == null }) event.cancel()
            }
        }
        if (config.fixGhostEntities && (event.entity is Monster || event.entity is Player)) {
            if (event.entity.id in hiddenEntityIds) event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(95, "misc.hideTemporaryArmorstands", "misc.hideTemporaryArmorStands")
    }
}
