package at.hannibal2.skyhanni.features.dungeon

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
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.event.jerry.highlightJerries) return

        val entity = event.entity
        val maxHealth = event.maxHealth
        val listOfLorenzColors = mapOf(2 to LorenzColor.WHITE, 3 to LorenzColor.GREEN, 4 to LorenzColor.BLUE, 5 to LorenzColor.DARK_PURPLE, 6 to LorenzColor.GOLD, 7 to LorenzColor.LIGHT_PURPLE) //2 and 7 are in case admins do a little trolling

        if (entity is EntityVillager && maxHealth < 7 && maxHealth > 2) {
            RenderLivingEntityHelper.setEntityColor(entity, listOfLorenzColors[maxHealth].toColor().withAlpha(20))
            { SkyHanniMod.feature.event.jerry.highlightJerries }
            RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.event.jerry.highlightJerries }
        }
    }
}