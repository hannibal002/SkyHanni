package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.ConfigUtils.toSet
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereWireframeInWorld

@SkyHanniModule
object SeaCreatureLootshareSphere {
    private val config get() = SkyHanniMod.feature.fishing

    private const val RANGE = 30.0f

    private var names = setOf<String>()
    private val seaCreatures = mutableSetOf<SeaCreatureData>()

    fun isInRange(pos: LorenzVec): Boolean = pos.distanceToPlayer() < RANGE

    @HandleEvent
    fun onSeaCreatureSpawn(event: SeaCreatureEvent.Spawn) = addMob(event.seaCreature)

    @HandleEvent
    fun onSeaCreatureRemove(event: SeaCreatureEvent.Remove) = seaCreatures.remove(event.seaCreature)

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lootshareRange) return
        for (seaCreature in seaCreatures) {
            if (!seaCreature.isLoaded()) continue
            val pos = seaCreature.pos ?: continue
            val color = if (seaCreature.isOwn || isInRange(pos)) LorenzColor.GREEN else LorenzColor.WHITE
            event.drawSphereWireframeInWorld(color.toColor(), pos, RANGE)
        }
    }

    @HandleEvent
    fun onConfigLoad() {
        updateNames()
        config.lootshareMobs.onToggle {
            updateNames()
            reloadMobs()
        }
    }

    private fun addMob(seaCreature: SeaCreatureData) {
        if (seaCreature.name in names) seaCreatures.add(seaCreature)
    }

    private fun reloadMobs() {
        seaCreatures.clear()
        SeaCreatureDetectionApi.getSeaCreatures().forEach(::addMob)
    }

    private fun updateNames() {
        names = config.lootshareMobs.toSet(false)
    }
}
