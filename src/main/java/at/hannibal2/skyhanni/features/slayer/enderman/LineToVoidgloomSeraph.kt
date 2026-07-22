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
    private val config get() = SlayerApi.config.endermen
    private val bosses = mutableSetOf<Mob>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (SlayerType.getByName(mob.name) != SlayerType.VOID) return
        if (!mob.belongsToPlayer()) return
        bosses += mob
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        bosses -= event.mob
    }

    @HandleEvent
    fun onWorldChange() = bosses.clear()

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!SlayerApi.isInAnyArea || !config.lineToBoss) return
        val seenMobs = bosses.filter { it.baseEntity.canBeSeen(30) && it.isAlive }
        seenMobs.forEach { mob ->
            event.drawLineToCrosshair(
                mob.baseEntity.getLorenzVec().up(),
                LorenzColor.AQUA.toChromaColor(),
                config.slayerLineWidth,
                true,
            )
        }
    }
}
