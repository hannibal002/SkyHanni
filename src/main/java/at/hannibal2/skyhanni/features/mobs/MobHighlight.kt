package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.monster.EntityEnderman
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
            if (maxHealth == 3_000 && entity is EntitySpider) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_BLUE.toColor().withAlpha(127))
                { SkyHanniMod.feature.mobs.arachneKeeperHighlight }
                RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.mobs.arachneKeeperHighlight }
            }
        }

        if (SkyHanniMod.feature.mobs.corleoneHighlighter) {
            if (maxHealth == 1_000_000 && entity is EntityOtherPlayerMP && entity.name == "Team Treasurite") {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_PURPLE.toColor().withAlpha(127))
                { SkyHanniMod.feature.mobs.corleoneHighlighter }
                RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.mobs.corleoneHighlighter }
            }
        }

        if (SkyHanniMod.feature.mobs.zealotBruiserHighlighter) {
            if ((maxHealth == 65_000 || maxHealth == 13_000) && entity is EntityEnderman) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_AQUA.toColor().withAlpha(127))
                { SkyHanniMod.feature.mobs.zealotBruiserHighlighter }
                RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.mobs.zealotBruiserHighlighter }
            }
        }

        if (SkyHanniMod.feature.mobs.specialZealotHighlighter) {
            if (maxHealth == 2_000 && entity is EntityEnderman) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_RED.toColor().withAlpha(50))
                { SkyHanniMod.feature.mobs.specialZealotHighlighter }
                RenderLivingEntityHelper.setNoHurtTime(entity) { SkyHanniMod.feature.mobs.specialZealotHighlighter }
            }
        }
    }
}