package at.hannibal2.hanni.features.nether.ashfang

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.compat.getAllEquipment
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object AshfangHider {

    private val config get() = AshfangManager.config.hide

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onRenderLiving(event: HanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!AshfangManager.active || !config.damageSplash) return

        if (DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!AshfangManager.active || !config.particles) return
        event.cancel()
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityArmorStand>) {
        if (!AshfangManager.active || !config.particles) return
        if (event.entity.getAllEquipment().any { it?.displayName == "Glowstone" }) event.cancel()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.hideDamageSplash", "crimsonIsle.ashfang.hide.damageSplash")
        event.move(2, "ashfang.hideParticles", "crimsonIsle.ashfang.hide.particles")
    }
}
