package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.EntityUtils.hasNameTagWith
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.CaveSpider
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Spider
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object MobHighlight {

    private val config get() = SkyHanniMod.feature.combat.mobs
    private var arachne: LivingEntity? = null
    private val toHighlightRunicMobs: HashSet<Mob> = hashSetOf()

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.isRunic) toHighlightRunicMobs.add(mob)
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        if (mob.isRunic) toHighlightRunicMobs.remove(mob)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!config.runicMobHighlight) return

        toHighlightRunicMobs.forEach {
            it.highlight(LorenzColor.LIGHT_PURPLE.toChromaColor()) { config.runicMobHighlight }
        }
        toHighlightRunicMobs.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {

        val entity = event.entity
        val baseMaxHealth = entity.baseMaxHealth
        if (config.corruptedMobHighlight && event.health == baseMaxHealth * 3) {
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                entity,
                LorenzColor.DARK_PURPLE.toColor().addAlpha(127),
            ) { config.corruptedMobHighlight }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {

        val entity = event.entity
        val maxHealth = event.maxHealth
        if (config.arachneKeeperHighlight && (maxHealth == 3_000 || maxHealth == 12_000) && entity is CaveSpider) {
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                entity,
                LorenzColor.DARK_BLUE.toColor().addAlpha(127),
            ) { config.arachneKeeperHighlight }
        }

        if (config.corleoneHighlighter && maxHealth == 1_000_000 && entity is RemotePlayer && entity.name.formattedTextCompatLessResets() == "Team Treasurite") {
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                entity,
                LorenzColor.DARK_PURPLE.toColor().addAlpha(127),
            ) { config.corleoneHighlighter }
        }

        if (entity is EnderMan) {
            val isZealot = maxHealth == 13_000 || maxHealth == 13_000 * 4 // runic
            val isBruiser = maxHealth == 65_000 || maxHealth == 65_000 * 4 // runic

            if (!(isZealot || isBruiser)) return

            if (config.zealotBruiserHighlighter) {
                RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                    entity,
                    LorenzColor.DARK_AQUA.toColor().addAlpha(127),
                ) { config.zealotBruiserHighlighter }
            }

            val heldItem = entity.getBlockInHand()?.block
            if (config.chestZealotHighlighter && heldItem == Blocks.ENDER_CHEST) {
                RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                    entity,
                    LorenzColor.GREEN.toColor().addAlpha(127),
                ) { config.chestZealotHighlighter }
            }

            if (config.specialZealotHighlighter && heldItem == Blocks.END_PORTAL_FRAME) {
                RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                    entity,
                    LorenzColor.DARK_RED.toColor().addAlpha(50),
                ) { config.specialZealotHighlighter }
            }
        }

        if (entity is Spider) {
            checkArachne(entity)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lineToArachne) return

        val arachne = arachne ?: return
        if (arachne.deceased || arachne.findHealthReal() <= 0) {
            this.arachne = null
            return
        }

        if (!arachne.canBeSeen(10)) return

        event.drawLineToEye(
            arachne.getLorenzVec().up(),
            LorenzColor.RED.toChromaColor(),
            config.lineToArachneWidth,
            true,
        )
    }

    @HandleEvent
    fun onWorldChange() {
        arachne = null
        toHighlightRunicMobs.clear()
    }

    private fun checkArachne(entity: Spider) {
        if (!config.arachneBossHighlighter && !config.lineToArachne) return

        if (!entity.hasNameTagWith(1, "[§7Lv300§8] §cArachne") &&
            !entity.hasNameTagWith(1, "[§7Lv300§8] §lArachne") &&
            !entity.hasNameTagWith(1, "[§7Lv500§8] §cArachne") &&
            !entity.hasNameTagWith(1, "[§7Lv500§8] §lArachne")
        ) return

        if (entity is CaveSpider) {
            markArachneMinis(entity)
        } else if (entity.baseMaxHealth == 20_000 || entity.baseMaxHealth == 100_000) {
            this.arachne = entity
            markArachne(entity)
        }
    }

    private fun markArachneMinis(entity: LivingEntity) {
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity,
            LorenzColor.GOLD.toColor().addAlpha(50),
        ) { config.arachneBossHighlighter }
    }

    private fun markArachne(entity: LivingEntity) {
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity,
            LorenzColor.RED.toColor().addAlpha(50),
        ) { config.arachneBossHighlighter }
    }
}
