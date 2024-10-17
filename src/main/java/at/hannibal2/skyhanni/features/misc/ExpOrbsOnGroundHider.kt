package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.item.EntityXPOrb
import at.hannibal2.skyhanni.api.event.HandleEvent

@SkyHanniModule
object ExpOrbsOnGroundHider {

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.hideExpBottles) return

        if (event.entity is EntityXPOrb) {
            event.cancel()
        }
    }
}
