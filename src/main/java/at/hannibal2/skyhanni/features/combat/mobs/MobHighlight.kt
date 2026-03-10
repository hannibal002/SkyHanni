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

        val (color, isEnabled) = when {
            name == "Boss Corleone" ->
                LorenzColor.DARK_PURPLE to config::corleoneHighlighter

            name == "Arachne's Keeper" ->
                LorenzColor.DARK_BLUE to config::arachneKeeperHighlight

            name == "Arachne's Brood" ->
                LorenzColor.GOLD to config::arachneBossHighlighter

            name == "Arachne" -> {
                arachne = mob
                LorenzColor.RED to config::arachneBossHighlighter
            }

            mob.isRunic ->
                LorenzColor.LIGHT_PURPLE to config::runicMobHighlight

            else -> return
        }

        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            mob.baseEntity,
            color.toColor().addAlpha(127),
        ) { isEnabled() }
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (arachne == event.mob) arachne = null
    }

    // TODO: change to use nametags instead
    // as this method does not work for mobs that spawn corrupted naturally
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
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (event.entity !is EnderMan) return

        val entity = event.entity

        val heldBlock = entity.getBlockInHand()?.block

        val (color, alpha, isEnabled) = when {
            heldBlock == Blocks.END_PORTAL_FRAME ->
                Triple(LorenzColor.DARK_RED, 50, config::specialZealotHighlighter)

            heldBlock == Blocks.ENDER_CHEST ->
                Triple(LorenzColor.GREEN, 127, config::chestZealotHighlighter)

            entity.isZealotOrBruiser() ->
                Triple(LorenzColor.DARK_AQUA, 127, config::zealotBruiserHighlighter)
            else -> return
        }

        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity,
            color.toColor().addAlpha(alpha),
        ) { isEnabled() }
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

    private fun LivingEntity.isZealotOrBruiser() = baseMaxHealth == 13_000 || baseMaxHealth == 65_000 ||
        baseMaxHealth == 13_000 * 4 || baseMaxHealth == 65_000 * 4 // runic
}
