package at.hannibal2.skyhanni.features.event.jerry

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.passive.EntityVillager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightJerries {

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!SkyHanniMod.feature.event.jerry.highlightJerries) return

        val entity = event.entity
        val maxHealth = event.maxHealth

        if (entity is EntityVillager) {
            if (maxHealth == 3) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.GREEN.toColor().withAlpha(20))
                { SkyHanniMod.feature.event.jerry.highlightJerries }
            }
            if (maxHealth == 4) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.BLUE.toColor().withAlpha(20))
                { SkyHanniMod.feature.event.jerry.highlightJerries }
            }
            if (maxHealth == 5) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_PURPLE.toColor().withAlpha(20))
                { SkyHanniMod.feature.event.jerry.highlightJerries }
            }
            if (maxHealth == 6) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.GOLD.toColor().withAlpha(20))
                { SkyHanniMod.feature.event.jerry.highlightJerries }
            }
        }
    }
}