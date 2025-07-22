package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.entity.EntityLiving

@SkyHanniModule
object HellionShieldHelper {

    val hellionShieldMobs = mutableMapOf<EntityLiving, HellionShield>()

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "slayer.blazeColoredMobs", "slayer.blazes.hellion.coloredMobs")
    }

    @HandleEvent
    fun onWorldChange() {
        hellionShieldMobs.clear()
    }

    fun EntityLiving.setHellionShield(shield: HellionShield?) {
        if (shield != null) {
            hellionShieldMobs[this] = shield
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                this,
                shield.color.toColor().addAlpha(80),
            ) { SkyBlockUtils.inSkyBlock && SlayerApi.config.blazes.hellion.coloredMobs }
        } else {
            hellionShieldMobs.remove(this)
            RenderLivingEntityHelper.removeCustomRender(this)
        }
    }
}
