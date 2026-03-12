package at.hannibal2.skyhanni.features.slayer.spider

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import net.minecraft.world.entity.Entity

@SkyHanniModule
object LineToSpiderSlayer {
    private val config get() = SlayerApi.config.spider
    private var bosses = mutableSetOf<Mob>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (SlayerType.getByName(mob.name) != SlayerType.TARANTULA) return
        bosses += mob
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        bosses -= event.mob
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        bosses.removeIf { it.baseEntity.deceased }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!SlayerApi.isInAnyArea) return
        if (!config.LineToSpiderSlayer) return
        for (mob in bosses) {
            if (!mob.baseEntity.canBeSeen(30) || !mob.belongsToPlayer()) continue
            event.drawLineToEye(
                mob.baseEntity.getLorenzVec().up(),
                LorenzColor.AQUA.toChromaColor(),
                config.slayerLineWidth,
                true,
            )
        }
    }
}


