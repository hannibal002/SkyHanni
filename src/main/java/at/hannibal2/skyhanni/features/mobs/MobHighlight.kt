package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.hasNameTagWith
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityCaveSpider
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntitySpider
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MobHighlight {
    private val config get() = SkyHanniMod.feature.combat.mobs

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val entity = event.entity
        val baseMaxHealth = entity.baseMaxHealth
        if (config.corruptedMobHighlight && event.health == baseMaxHealth * 3) {
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_PURPLE.toColor().withAlpha(127))
            { config.corruptedMobHighlight }
            RenderLivingEntityHelper.setNoHurtTime(entity) { config.corruptedMobHighlight }
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val entity = event.entity
        val maxHealth = event.maxHealth
        if (config.arachneKeeperHighlight && (maxHealth == 3_000 || maxHealth == 12_000) && entity is EntityCaveSpider) {
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_BLUE.toColor().withAlpha(127))
            { config.arachneKeeperHighlight }
            RenderLivingEntityHelper.setNoHurtTime(entity) { config.arachneKeeperHighlight }
        }

        if (config.corleoneHighlighter && maxHealth == 1_000_000 && entity is EntityOtherPlayerMP && entity.name == "Team Treasurite") {
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_PURPLE.toColor().withAlpha(127))
            { config.corleoneHighlighter }
            RenderLivingEntityHelper.setNoHurtTime(entity) { config.corleoneHighlighter }
        }

        if (config.zealotBruiserHighlighter) {
            val isZealot = maxHealth == 13_000 || maxHealth == 13_000 * 3 // runic
            val isBruiser = maxHealth == 65_000 || maxHealth == 65_000 * 3 // runic
            if ((isZealot || isBruiser) && entity is EntityEnderman) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_AQUA.toColor().withAlpha(127))
                { config.zealotBruiserHighlighter }
                RenderLivingEntityHelper.setNoHurtTime(entity) { config.zealotBruiserHighlighter }
            }
        }

        if (config.specialZealotHighlighter && maxHealth == 2_000 && entity is EntityEnderman) {
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.DARK_RED.toColor().withAlpha(50))
            { config.specialZealotHighlighter }
            RenderLivingEntityHelper.setNoHurtTime(entity) { config.specialZealotHighlighter }
        }

        if (config.arachneBossHighlighter && entity is EntitySpider) {
            checkArachne(entity)
        }
    }

    private fun checkArachne(entity: EntitySpider) {
        if (!entity.hasNameTagWith(1, "[§7Lv300§8] §cArachne") &&
            !entity.hasNameTagWith(1, "[§7Lv300§8] §lArachne") &&
            !entity.hasNameTagWith(1, "[§7Lv500§8] §cArachne") &&
            !entity.hasNameTagWith(1, "[§7Lv500§8] §lArachne")
        ) return

        if (entity is EntityCaveSpider) {
            markArachneMinis(entity)
        } else if (entity.baseMaxHealth == 20_000 || entity.baseMaxHealth == 100_000) {
            markArachne(entity)
        }
    }

    private fun markArachneMinis(entity: EntityLivingBase) {
        RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.GOLD.toColor().withAlpha(50))
        { config.arachneBossHighlighter }
        RenderLivingEntityHelper.setNoHurtTime(entity) { config.arachneBossHighlighter }
    }

    private fun markArachne(entity: EntityLivingBase) {
        RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.RED.toColor().withAlpha(50))
        { config.arachneBossHighlighter }
        RenderLivingEntityHelper.setNoHurtTime(entity) { config.arachneBossHighlighter }
    }
}
