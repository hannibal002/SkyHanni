package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.entity.monster.EntitySpider
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MobHighlight {

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val entity = event.entity
        val baseMaxHealth = entity.baseMaxHealth
        if (SkyHanniMod.feature.mobs.corruptedMobHighlight) {
            if (event.health == baseMaxHealth * 3) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_PURPLE.toColor().withAlpha(127))
                { SkyHanniMod.feature.mobs.corruptedMobHighlight }
                RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.mobs.corruptedMobHighlight }
            }
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val entity = event.entity
        val maxHealth = event.maxHealth
        if (SkyHanniMod.feature.mobs.arachneKeeperHighlight) {
            if (entity is EntitySpider) {
                if (maxHealth == 3_000) {
                    RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_BLUE.toColor().withAlpha(127))
                    { SkyHanniMod.feature.mobs.arachneKeeperHighlight }
                    RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.mobs.arachneKeeperHighlight }
                }
            }
        }
    }
}