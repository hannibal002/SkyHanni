package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye

@SkyHanniModule
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
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
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
