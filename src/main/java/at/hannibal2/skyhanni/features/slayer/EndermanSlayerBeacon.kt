package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EndermanSlayerBeacon {

    private val endermenWithBeacons = mutableListOf<EntityEnderman>()
    private val flyingBeacons = mutableListOf<EntityArmorStand>()
    private val sittingBeacon = mutableListOf<LorenzVec>()

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        val entity = event.entity
        if (entity in endermenWithBeacons || entity in flyingBeacons) return

        if (entity is EntityEnderman) {
            if (hasBeaconInHand(entity) && canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                endermenWithBeacons.add(entity)
            }
        }

        if (entity is EntityArmorStand) {
            val stack = entity.inventory[4] ?: return
            if (stack.name == "Beacon" && canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                flyingBeacons.add(entity)
            }
        }
    }

    private fun hasBeaconInHand(entity: EntityEnderman): Boolean {
        val heldBlockState = entity.heldBlockState ?: return false
        val block = heldBlockState.block ?: return false
        return block == Blocks.beacon
    }

    private fun canSee(a: LorenzVec, b: LorenzVec): Boolean = LocationUtils.canSee(a, b) || a.distance(b) < 15

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return

        if (event.entity in flyingBeacons) {
            event.color = LorenzColor.DARK_RED.toColor().withAlpha(1)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        endermenWithBeacons.removeIf { it.isDead || !hasBeaconInHand(it) }

        endermenWithBeacons.map { it.getLorenzVec().add(-0.5, 0.2, -0.5) }
            .forEach { event.drawColor(it, LorenzColor.DARK_RED, alpha = 1f) }

        for (location in sittingBeacon.toMutableList()) {
            event.drawColor(location, LorenzColor.DARK_RED, alpha = 1f)
            event.drawString(location.add(0.5, 0.5, 0.5), "§4Beacon", true)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onPacketReceive(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet
        if (packet is S23PacketBlockChange) {
            val location = packet.blockPosition.toLorenzVec()
            if (packet.blockState?.block == Blocks.beacon) {
                val armorStand = flyingBeacons.find { location.distance(it.getLorenzVec()) < 3 }
                if (armorStand != null) {
                    flyingBeacons.remove(armorStand)
                    sittingBeacon.add(location)
                }
            } else {
                if (location in sittingBeacon) {
                    sittingBeacon.remove(location)
                }
            }
        }
    }

    private fun isEnabled(): Boolean = LorenzUtils.inSkyBlock &&
            SkyHanniMod.feature.slayer.slayerEndermanBeacon &&
            LorenzUtils.skyBlockIsland == IslandType.THE_END &&
            DamageIndicatorManager.isBossSpawned(
                BossType.SLAYER_ENDERMAN_2,
                BossType.SLAYER_ENDERMAN_3,
                BossType.SLAYER_ENDERMAN_4
            )

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        endermenWithBeacons.clear()
        flyingBeacons.clear()
        sittingBeacon.clear()
    }
}