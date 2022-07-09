package at.lorenz.mod.misc

import at.lorenz.mod.LorenzMod
import at.lorenz.mod.events.CheckRenderEntityEvent
import at.lorenz.mod.utils.LorenzUtils
import net.minecraft.entity.item.EntityXPOrb
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ExpBottleOnGroundHider {
    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inSkyblock) return
        if (!LorenzMod.feature.misc.hideExpBottles) return

        if (event.entity is EntityXPOrb) {
            event.isCanceled = true
        }
    }
}