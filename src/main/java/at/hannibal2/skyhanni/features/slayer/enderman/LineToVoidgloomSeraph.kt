package at.hannibal2.skyhanni.features.slayer.enderman

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
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair

@SkyHanniModule
object LineToVoidgloomSeraph {
    private val config get() = SlayerApi.config.endermen.lineToBoss
    private val bosses = mutableSetOf<Mob>()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (SlayerType.getByName(mob.name) != SlayerType.VOID) return
        if (!mob.belongsToPlayer()) return
        bosses += mob
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        bosses -= event.mob
    }

    @HandleEvent
    private fun onWorldChange() = bosses.clear()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!SlayerApi.isInAnyArea || !config.showLine) return
        val seenMobs = bosses.filter { it.baseEntity.canBeSeen(30) && it.isAlive }
        seenMobs.forEach { mob ->
            event.drawLineToCrosshair(
                mob.baseEntity.getLorenzVec().up(),
                config.color,
                config.lineWidth,
                true,
            )
        }
    }
}
