package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.EntityLiving
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HellionShieldHelper {

    companion object {

        val hellionShieldMobs = mutableMapOf<EntityLiving, HellionShield>()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "slayer.blazeColoredMobs", "slayer.blazes.hellion.coloredMobs")
    }
}

fun EntityLiving.setHellionShield(shield: HellionShield?) {
    if (shield != null) {
        HellionShieldHelper.hellionShieldMobs[this] = shield
        RenderLivingEntityHelper.setNoHurtTime(this) { LorenzUtils.inSkyBlock && SkyHanniMod.feature.slayer.blazes.hellion.coloredMobs }
        RenderLivingEntityHelper.setEntityColor(
            this,
            shield.color.toColor().withAlpha(80)
        ) { LorenzUtils.inSkyBlock && SkyHanniMod.feature.slayer.blazes.hellion.coloredMobs }
    } else {
        HellionShieldHelper.hellionShieldMobs.remove(this)
        RenderLivingEntityHelper.removeNoHurtTime(this)
        RenderLivingEntityHelper.removeEntityColor(this)
    }
}
