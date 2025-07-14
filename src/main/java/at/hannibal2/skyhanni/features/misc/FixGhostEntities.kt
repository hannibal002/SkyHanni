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
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
//#if MC < 1.21
import net.minecraft.network.play.server.S0FPacketSpawnMob
//#endif
import net.minecraft.network.play.server.S13PacketDestroyEntities

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
        if (KuudraApi.inKuudra || !PlatformUtils.IS_LEGACY) return

        when (val packet = event.packet) {
            is S0CPacketSpawnPlayer -> {
                if (packet.entityID in recentlyRemovedEntities) {
                    hiddenEntityIds.add(packet.entityID)
                }
                recentlySpawnedEntities.addLast(packet.entityID)
            }
            //#if MC < 1.21
            is S0FPacketSpawnMob -> {
                if (packet.entityID in recentlyRemovedEntities) {
                    hiddenEntityIds.add(packet.entityID)
                }
                recentlySpawnedEntities.addLast(packet.entityID)
            }
            //#endif
            is S13PacketDestroyEntities -> {
                for (entityID in packet.entityIDs) {
                    // ignore entities that got properly spawned and then removed
                    if (entityID !in recentlySpawnedEntities) {
                        recentlyRemovedEntities.addLast(entityID)
                        if (recentlyRemovedEntities.size == 10) {
                            recentlyRemovedEntities.removeFirst()
                        }
                    }
                    hiddenEntityIds.remove(entityID)
                }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (config.hideTemporaryArmorStands && event.entity is EntityArmorStand) {
            with(event.entity) {
                if (ticksExisted < 10 && isDefaultValue() && getAllEquipment().all { it == null }) {
                    event.cancel()
                }
            }
        }
        if (config.fixGhostEntities && (event.entity is EntityMob || event.entity is EntityPlayer)) {
            with(event.entity) {
                if (hiddenEntityIds.contains(entityId)) {
                    event.cancel()
                }
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(95, "misc.hideTemporaryArmorstands", "misc.hideTemporaryArmorStands")
    }
}
