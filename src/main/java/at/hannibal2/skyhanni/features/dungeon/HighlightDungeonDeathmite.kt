package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.monster.EntitySilverfish
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightDungeonDeathmite {

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.highlightDeathmites) return

        val entity = event.entity
        val maxHealth = event.maxHealth

        if (entity is EntitySilverfish && maxHealth == 1_000_000_000) {
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_RED.toColor().withAlpha(20))
            { SkyHanniMod.feature.dungeon.highlightDeathmites }
            RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.dungeon.highlightDeathmites }
        }
    }
}