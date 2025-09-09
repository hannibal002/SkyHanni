package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.entity.ExperienceOrbEntity

@SkyHanniModule
object ExpOrbsOnGroundHider {

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<ExperienceOrbEntity>) {
        if (!SkyHanniMod.feature.misc.hideExpBottles) return
        event.cancel()
    }
}
