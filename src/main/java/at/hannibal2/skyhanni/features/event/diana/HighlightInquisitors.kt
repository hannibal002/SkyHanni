package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightInquisitors {

    private val config get() = SkyHanniMod.feature.event.diana

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.highlightInquisitors) return

        val entity = event.entity

        if (entity is EntityPlayer && entity.name == "Minos Inquisitor") {
            val color = config.color.toChromaColorInt()
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(entity, color) { config.highlightInquisitors }
        }
    }
}
