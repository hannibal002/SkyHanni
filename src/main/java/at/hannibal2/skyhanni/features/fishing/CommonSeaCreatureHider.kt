package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyTickEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi.seaCreature
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import net.minecraft.world.entity.LivingEntity

@SkyHanniModule
object CommonSeaCreatureHider {
    private val config get() = SkyHanniMod.feature.fishing.commonSeaCreatureHider

    @HandleEvent
    fun onEntityTransparency(event: EntityTransparencyTickEvent<LivingEntity>) {
        if (!config.enabled) return
        if (config.onlyWhileFishing && !FishingApi.holdingRod) return

        val entity = event.entity
        val seaCreature = entity.mob?.seaCreature ?: return

        if (!seaCreature.seaCreature.rare) {
            event.newTransparency = config.transparency;
        }
    }
}
