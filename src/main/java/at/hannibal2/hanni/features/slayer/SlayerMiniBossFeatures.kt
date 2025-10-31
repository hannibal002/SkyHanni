package at.hannibal2.hanni.features.slayer

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils.canBeSeen
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawLineToEye

@HanniModule
object SlayerMiniBossFeatures {

    private val config get() = SlayerApi.config
    private var miniBosses = mutableSetOf<Mob>()

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (!SlayerMiniBossType.isMiniboss(mob.name)) return
        miniBosses += mob
        // TODO config option for color
        if (config.slayerMinibossHighlight) mob.highlight(LorenzColor.AQUA.toColor())
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        miniBosses -= event.mob
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!SlayerApi.isInAnyArea) return
        if (!config.slayerMinibossLine) return
        for (mob in miniBosses) {
            if (!mob.baseEntity.canBeSeen(10)) continue
            event.drawLineToEye(
                mob.baseEntity.getLorenzVec().up(),
                LorenzColor.AQUA.toChromaColor(),
                config.slayerMinibossLineWidth,
                true,
            )
        }
    }
}
