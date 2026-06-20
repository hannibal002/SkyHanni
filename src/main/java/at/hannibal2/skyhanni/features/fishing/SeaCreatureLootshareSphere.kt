package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SeaCreatureSettings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LootshareUtils
import at.hannibal2.skyhanni.utils.LootshareUtils.isInLootshareRange
import at.hannibal2.skyhanni.utils.LorenzColor

@SkyHanniModule
object SeaCreatureLootshareSphere {
    private val config get() = SkyHanniMod.feature.fishing

    private val seaCreatures = mutableSetOf<LivingSeaCreatureData>()

    private val existingCircles = mutableSetOf<LorenzVec>()

    @HandleEvent
    fun onSeaCreatureSpawn(event: SeaCreatureEvent.Spawn) = addMob(event.seaCreature)

    @HandleEvent
    fun onSeaCreatureRemove(event: SeaCreatureEvent.Remove) = seaCreatures.remove(event.seaCreature)

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lootshareRange) return
        existingCircles.clear()
        var circleCount = 0
        for (seaCreature in seaCreatures) {
            if (!seaCreature.exists()) continue
            val pos = seaCreature.pos ?: continue
            val color = if (seaCreature.isOwn || isInLootshareRange(pos)) LorenzColor.GREEN else LorenzColor.WHITE
            existingCircles.forEach {
                if (it.distance(pos) < 10) circleCount++
            }
            if (circleCount > 2) continueevent.drawSphereWireframeInWorld(color.toColor(), pos, RANGE)
            existingCircles.add(pos)
        }
    }

    private fun addMob(seaCreature: LivingSeaCreatureData) {
        if (SeaCreatureSettings.getConfig(seaCreature)?.shouldRenderLootshare == true) seaCreatures.add(seaCreature)
    }
}
