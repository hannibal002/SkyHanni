package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.item.EntityXPOrb
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ExpBottleOnGroundHider {
    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.misc.hideExpBottles) return

        if (event.entity is EntityXPOrb) {
            event.isCanceled = true
        }
    }
}