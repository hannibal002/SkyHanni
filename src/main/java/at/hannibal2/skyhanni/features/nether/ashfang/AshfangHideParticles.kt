package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AshfangHideParticles {
    private var nearAshfang = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.repeatSeconds(3)) {
            nearAshfang = DamageIndicatorManager.getDistanceTo(BossType.NETHER_ASHFANG) < 40
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: ReceiveParticleEvent) {
        if (isEnabled()) {
            event.isCanceled = true
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

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.hideParticles", "crimsonIsle.ashfang.hide.particles")
    }

    private fun isEnabled() =
        LorenzUtils.inSkyBlock && SkyHanniMod.feature.crimsonIsle.ashfang.hide.particles && nearAshfang
}