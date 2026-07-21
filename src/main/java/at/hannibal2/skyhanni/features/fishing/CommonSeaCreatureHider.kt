package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyActiveEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyTickEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi.seaCreature
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import net.minecraft.world.entity.LivingEntity

@SkyHanniModule
object CommonSeaCreatureHider {
    private val config get() = SkyHanniMod.feature.fishing.commonSeaCreatureHider

    @HandleEvent
    fun onEntityTransparencyActive(event: EntityTransparencyActiveEvent) {
        event.setActive(isEnabled())
    }

    @HandleEvent
    fun onEntityTransparency(event: EntityTransparencyTickEvent<LivingEntity>) {
        if (!isEnabled()) return

        val entity = event.entity
        val seaCreature = entity.mob?.seaCreature?.seaCreature ?: return

        if (!seaCreature.rare) {
            event.newTransparency = config.transparency
        }
    }

    fun isEnabled() =
        config.enabled &&
            FishingApi.isFishing(checkRodInHand = false) &&
            (!config.onlyWhileHoldingRod || FishingApi.holdingRod)
}
