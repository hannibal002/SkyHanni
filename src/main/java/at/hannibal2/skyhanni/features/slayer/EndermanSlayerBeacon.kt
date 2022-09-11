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
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EndermanSlayerBeacon {

    private val endermens = mutableListOf<EntityEnderman>()
    private val armorStands = mutableListOf<EntityArmorStand>()
    private val blocks = mutableListOf<LorenzVec>()

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        val entity = event.entity
        if (entity in endermens || entity in armorStands) return

        if (entity is EntityEnderman) {
            if (hasBeaconInHand(entity) && canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                endermens.add(entity)
            }
        }

        if (entity is EntityArmorStand) {
            val stack = entity.inventory[4] ?: return
            if (stack.name == "Beacon" && canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                armorStands.add(entity)
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

        if (event.entity in armorStands) {
            event.color = LorenzColor.DARK_RED.toColor().withAlpha(1)
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        endermens.removeIf { it.isDead || !hasBeaconInHand(it) }

        endermens.map { it.getLorenzVec().add(-0.5, 0.2, -0.5) }
            .forEach { event.drawColor(it, LorenzColor.DARK_RED, alpha = 1f) }

        for (location in blocks) {
            event.drawColor(location, LorenzColor.DARK_RED, alpha = 1f)
            event.drawString(location.add(0.5, 0.5, 0.5), "§4Beacon", true)
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
                val armorStand = armorStands.find { vec.distance(it.getLorenzVec()) < 3 }
                if (armorStand != null) {
                    armorStands.remove(armorStand)
                    blocks.add(vec)
                }
            } else {
                if (vec in blocks) {
                    blocks.remove(vec)
                }
            }
        }
    }

    private fun isEnabled(): Boolean = LorenzUtils.inSkyblock && SkyHanniMod.feature.misc.slayerEndermanBeacon &&
            LorenzUtils.skyBlockIsland == "The End" &&
            (DamageIndicatorManager.isBossSpawned(BossType.SLAYER_ENDERMAN_2) ||
                    DamageIndicatorManager.isBossSpawned(BossType.SLAYER_ENDERMAN_3) ||
                    DamageIndicatorManager.isBossSpawned(BossType.SLAYER_ENDERMAN_4))

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        endermens.clear()
        armorStands.clear()
        blocks.clear()
    }
}