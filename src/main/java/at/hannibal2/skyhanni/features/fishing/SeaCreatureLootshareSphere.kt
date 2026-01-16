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

    private fun addMob(seaCreature: SeaCreatureData) {
        if (scSpecificConfig[seaCreature.name]?.shouldRenderLootshare == true) seaCreatures.add(seaCreature)
    }

}
