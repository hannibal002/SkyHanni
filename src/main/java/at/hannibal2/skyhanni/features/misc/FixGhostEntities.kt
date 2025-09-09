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
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
//#if MC < 1.21
//$$ import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket
//#endif
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket

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
            is EntitySpawnS2CPacket -> {
                if (packet.entityId in recentlyRemovedEntities) {
                    hiddenEntityIds.add(packet.entityId)
                }
                recentlySpawnedEntities.addLast(packet.entityId)
            }
            //#if MC < 1.21
            //$$ is MobSpawnS2CPacket -> {
            //$$     if (packet.id in recentlyRemovedEntities) {
            //$$         hiddenEntityIds.add(packet.id)
            //$$     }
            //$$     recentlySpawnedEntities.addLast(packet.id)
            //$$ }
            //#endif
            is EntitiesDestroyS2CPacket -> {
                for (entityID in packet.entityIds) {
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
        if (config.hideTemporaryArmorStands && event.entity is ArmorStandEntity) {
            with(event.entity) {
                if (age < 10 && isDefaultValue() && getAllEquipment().all { it == null }) {
                    event.cancel()
                }
            }
        }
        if (config.fixGhostEntities && (event.entity is HostileEntity || event.entity is PlayerEntity)) {
            with(event.entity) {
                if (hiddenEntityIds.contains(id)) {
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
