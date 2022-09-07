package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
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
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EndermanSlayerBeacon {

    private val endermans = mutableListOf<EntityEnderman>()
    private val armorStands = mutableListOf<EntityArmorStand>()
    private val blocks = mutableListOf<LorenzVec>()

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (isEnabled()) {
            findEntities(event.entity)
        }
    }

    private fun hasBeaconInHand(entity: EntityEnderman): Boolean {
        val heldBlockState = entity.heldBlockState
        if (heldBlockState != null) {
            val block = heldBlockState.block
            if (block != null) {
                if (block == Blocks.beacon) {
                    return true
                }
            }
        }

        return false
    }

    private fun findEntities(entity: Entity) {
        if (entity in endermans) return
        if (entity in armorStands) return

        if (entity is EntityEnderman) {
            if (hasBeaconInHand(entity)) {
                if (LocationUtils.canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                    endermans.add(entity)
                }
            }
        }

        if (entity is EntityArmorStand) {
            val stack = entity.inventory[4] ?: return
            if (stack.name == "Beacon") {
                if (LocationUtils.canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                    armorStands.add(entity)
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return

        if (event.entity in armorStands) {
            event.color = LorenzColor.DARK_RED.toColor().withAlpha(1)
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        endermans.removeIf { it.isDead || !hasBeaconInHand(it) }

        for (enderman in endermans) {
            val location = enderman.getLorenzVec().add(-0.5, 0.2, -0.5)
            event.drawColor(location, LorenzColor.DARK_RED, alpha = 1f)
        }

        for (location in blocks) {
            event.drawColor(location, LorenzColor.DARK_RED, alpha = 1f)
            event.drawString(location.add(0.5, -0.5, 0.5), "Beacon", true)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet
        if (packet is S23PacketBlockChange) {
            val vec = packet.blockPosition.toLorenzVec()
            val block = packet.blockState.block
            if (block == Blocks.beacon) {
                if (armorStands.any { vec.distance(it.getLorenzVec()) < 3 }) {
                    blocks.add(vec)
                }
            } else {
                if (vec in blocks) {
                    blocks.remove(vec)
                }
            }
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.misc.slayerEndermanBeacon &&
                LorenzUtils.skyBlockIsland == "The End" &&
                (DamageIndicatorManager.isBossSpawned(BossType.SLAYER_ENDERMAN_2) ||
                        DamageIndicatorManager.isBossSpawned(BossType.SLAYER_ENDERMAN_3) ||
                        DamageIndicatorManager.isBossSpawned(BossType.SLAYER_ENDERMAN_4))
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        endermans.clear()
        armorStands.clear()
        blocks.clear()
    }
}