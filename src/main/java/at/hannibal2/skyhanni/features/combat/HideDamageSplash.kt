package at.hannibal2.hanni.features.combat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object HideDamageSplash {

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityArmorStand>) {
        if (!HanniMod.feature.combat.hideDamageSplash) return

        if (DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.hideDamageSplash", "combat.hideDamageSplash")
    }
}
