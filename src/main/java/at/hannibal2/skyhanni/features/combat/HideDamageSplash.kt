package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.EntityLivingBase

@SkyHanniModule
object HideDamageSplash {

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<EntityLivingBase>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.combat.hideDamageSplash) return

        if (DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.hideDamageSplash", "combat.hideDamageSplash")
    }
}
