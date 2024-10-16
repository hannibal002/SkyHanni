package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.name
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object AshfangHider {

    private val config get() = AshfangManager.config.hide

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!AshfangManager.active || !config.damageSplash) return

        if (DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.cancel()
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!AshfangManager.active || !config.particles) return
        event.cancel()
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!AshfangManager.active || !config.particles) return
        val entity = event.entity as? EntityArmorStand ?: return
        if (entity.inventory.any { it?.name == "Glowstone" }) event.cancel()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.hideDamageSplash", "crimsonIsle.ashfang.hide.damageSplash")
        event.move(2, "ashfang.hideParticles", "crimsonIsle.ashfang.hide.particles")
    }
}
