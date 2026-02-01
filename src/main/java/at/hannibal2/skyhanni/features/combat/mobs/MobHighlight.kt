package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object MobHighlight {

    private val config get() = SkyHanniMod.feature.combat.mobs
    private var arachne: Mob? = null

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        val name = mob.name

        val color = when {
            name == "Boss Corleone" && config.corleoneHighlighter -> LorenzColor.DARK_PURPLE

            name == "Arachne's Keeper" && config.arachneKeeperHighlight -> LorenzColor.DARK_BLUE
            name == "Arachne's Brood" && config.arachneBossHighlighter -> LorenzColor.GOLD
            name == "Arachne" && config.arachneBossHighlighter -> {
                arachne = mob
                LorenzColor.RED
            }

            mob.isRunic && config.runicMobHighlight -> LorenzColor.LIGHT_PURPLE

            else -> return
        }.toChromaColor()

        mob.highlight(color)
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (arachne == event.mob) arachne = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!config.corruptedMobHighlight) return

        val entity = event.entity
        if (!entity.isCorrupted()) return

        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity,
            LorenzColor.DARK_PURPLE.toColor().addAlpha(127),
        ) { config.corruptedMobHighlight }
    }

    // Mob detection isn't used here to allow for highlighting Zealots from further away.
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onEntityJoinWorld(event: EntityMaxHealthUpdateEvent) {
        if (event.entity !is EnderMan) return

        val entity = event.entity

        val heldBlock = entity.getBlockInHand()?.block

        val color = when {
            heldBlock == Blocks.END_PORTAL_FRAME && config.specialZealotHighlighter -> LorenzColor.DARK_RED
            heldBlock == Blocks.ENDER_CHEST && config.chestZealotHighlighter -> LorenzColor.GREEN
            entity.isZealotOrBruiser() && config.zealotBruiserHighlighter -> LorenzColor.DARK_AQUA
            else -> return
        }

        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity,
            color.toColor().addAlpha(127),
        ) { true }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lineToArachne) return

        val arachne = arachne ?: return

        if (!arachne.canBeSeen(10)) return

        event.drawLineToEye(
            arachne.centerCords,
            LorenzColor.RED.toChromaColor(),
            config.lineToArachneWidth,
            true,
        )
    }

    private fun LivingEntity.isZealotOrBruiser() = baseMaxHealth == 13_000 || baseMaxHealth == 65_000
}
