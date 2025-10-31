package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.entity.item.EntityXPOrb

@HanniModule
object ExpOrbsOnGroundHider {

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityXPOrb>) {
        if (!HanniMod.feature.misc.hideExpBottles) return
        event.cancel()
    }
}
