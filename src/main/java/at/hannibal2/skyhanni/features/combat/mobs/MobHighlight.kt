package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object MobHighlight {

    private val config get() = SkyHanniMod.feature.combat.mobs
    private var arachne: Mob? = null

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!LorenzUtils.inSkyBlock || !config.corruptedMobHighlight) return
        val mob = event.entity.mob ?: return
        if (mob.isCorrupted) mob.highlight(LorenzColor.DARK_PURPLE)
    }

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob

        when {
            mob.isRunic -> if (config.runicMobHighlighter) mob.highlight(LorenzColor.LIGHT_PURPLE)
            mob.isCorrupted -> if (config.corruptedMobHighlight) mob.highlight(LorenzColor.DARK_PURPLE)
        }

        // TODO: get the correct names
        when (mob.name) {
            "Arachne Keeper" -> if (config.arachneKeeperHighlight) mob.highlight(LorenzColor.DARK_BLUE)
            "Corleone" -> if (config.corleoneHighlighter) mob.highlight(LorenzColor.DARK_PURPLE)
            "Zealot", "Zealot Bruiser" -> {
                // TODO: check whats the mob name of special zealots
                val heldBlock = (mob.baseEntity as? EntityEnderman)?.getBlockInHand()?.block

                val color = when {
                    heldBlock == Blocks.end_portal_frame && config.specialZealotHighlighter -> LorenzColor.DARK_RED
                    heldBlock == Blocks.ender_chest && config.chestZealotHighlighter -> LorenzColor.GREEN
                    else -> LorenzColor.DARK_AQUA
                }

                mob.highlight(color)
            }

            "Arachne" -> {
                arachne = mob
                if (config.arachneBossHighlighter) mob.highlight(LorenzColor.RED, 50)
            }
            // TODO: i have no fucking idea what the mob name for this could even be
            "Arachne Mini" -> if (config.arachneBossHighlighter) mob.highlight(LorenzColor.GOLD, 50)
        }
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (arachne == event.mob) arachne = null
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock || !config.lineToArachne) return

        val arachne = arachne?.baseEntity ?: return

        if (arachne.distanceToPlayer() > 10) return
        event.draw3DLine(
            event.exactPlayerEyeLocation(),
            arachne.getLorenzVec().up(1.0),
            LorenzColor.RED.toColor(),
            5,
            true,
        )
    }
}
