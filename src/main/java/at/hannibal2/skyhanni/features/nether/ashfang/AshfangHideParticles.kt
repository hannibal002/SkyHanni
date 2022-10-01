package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.PlayParticleEvent
import at.hannibal2.skyhanni.events.SpawnParticleEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class AshfangHideParticles {

    var tick = 0
    var hideParticles = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyblock) return

        if (tick++ % 60 == 0) {
            val distance = DamageIndicatorManager.getDistanceTo(BossType.NETHER_ASHFANG)
            hideParticles = distance < 40
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: PlayParticleEvent) {
        if (!isEnabled()) return

        event.isCanceled = true
    }

    @SubscribeEvent
    fun onSpawnParticle(event: SpawnParticleEvent) {
        if (!isEnabled()) return


        when (event.callerClass) {
            "net.minecraft.block.BlockFire",
            "net.minecraft.entity.monster.EntityBlaze",
            "net.minecraft.entity.projectile.EntityFireball",
            -> {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return

        val entity = event.entity
        if (entity is EntityArmorStand) {
            for (stack in entity.inventory) {
                if (stack == null) continue
                val name = stack.name ?: continue
                if (name == "§aFairy Souls") continue
                if (name == "Glowstone") {
                    event.isCanceled = true
                }
            }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyblock && SkyHanniMod.feature.ashfang.hideParticles && hideParticles
}