package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.monster.EntitySilverfish
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightJerries {

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.event.jerry.highlightJerries) return

        val entity = event.entity
        val maxHealth = event.maxHealth
        val listOfLorenzColors = [LorenzColor.RED, LorenzColor.RED, LorenzColor.WHITE, LorenzColor.GREEN, LorenzColor.BLUE, LorenzColor.DARK_PURPLE, LorenzColor.GOLD, LorenzColor.LIGHT_PURPLE]

        if (entity is EntityVillager && maxHealth < 7 && maxHealth > 2) {
            RenderLivingEntityHelper.setEntityColor(entity, listOfLorenzColors[maxHealth].toColor().withAlpha(20))
            { SkyHanniMod.feature.event.jerry.highlightJerries }
            RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.event.jerry.highlightJerries }
        }
    }
}