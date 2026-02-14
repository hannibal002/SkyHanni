package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereWireframeInWorld

@SkyHanniModule
object SeaCreatureLootshareSphere {
    private val config get() = SkyHanniMod.feature.fishing
    private val scSpecificConfig get() = SkyHanniMod.seaCreatureStorage.specificSeaCreatureConfigStorage

    private const val RANGE = 30.0f

    private val seaCreatures = mutableSetOf<LivingSeaCreatureData>()
    private val existingCircles = mutableSetOf<LorenzVec>()

    fun isInRange(pos: LorenzVec): Boolean = pos.distanceToPlayer() < RANGE

    @HandleEvent
    fun onSeaCreatureSpawn(event: SeaCreatureEvent.Spawn) = addMob(event.seaCreature)

    @HandleEvent
    fun onSeaCreatureRemove(event: SeaCreatureEvent.Remove) = seaCreatures.remove(event.seaCreature)

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lootshareRange) return
        existingCircles.clear()
        for (seaCreature in seaCreatures) {
            if (!seaCreature.exists()) continue
            val pos = seaCreature.pos ?: continue
            var circleCount = 0
            existingCircles.forEach {
                if (it.distance(pos) < 10) circleCount++
            }
            if (circleCount > 2) continue
            val color = if (seaCreature.isOwn || isInRange(pos)) LorenzColor.GREEN else LorenzColor.WHITE
            event.drawSphereWireframeInWorld(color.toColor(), pos, RANGE)
            existingCircles.add(pos)
        }
    }

    private fun addMob(seaCreature: LivingSeaCreatureData) {
        if (scSpecificConfig[seaCreature.name]?.shouldRenderLootshare == true) seaCreatures.add(seaCreature)
    }

}
