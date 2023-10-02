package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.EntityLiving
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HellionShieldHelper {

    companion object {
        val hellionShieldMobs = mutableMapOf<EntityLiving, HellionShield>()
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.slayer.blaze.hellion.coloredMobs) return

        val shield = hellionShieldMobs.getOrDefault(event.entity, null) ?: return
        event.color = shield.color.toColor().withAlpha(80)
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.slayer.blaze.hellion.coloredMobs) return

        hellionShieldMobs.getOrDefault(event.entity, null) ?: return
        event.shouldReset = true
    }
}

fun EntityLiving.setHellionShield(shield: HellionShield?) {
    if (shield != null) {
        HellionShieldHelper.hellionShieldMobs[this] = shield
    } else {
        HellionShieldHelper.hellionShieldMobs.remove(this)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent){
        event.move(4, "slayer.blazeColoredMobs", "slayer.blaze.hellion.coloredMobs")
    }
}