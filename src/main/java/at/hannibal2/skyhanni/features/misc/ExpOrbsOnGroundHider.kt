package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.item.EntityXPOrb

@SkyHanniModule
object ExpOrbsOnGroundHider {

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<EntityXPOrb>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.hideExpBottles) return

        event.cancel()
    }
}
