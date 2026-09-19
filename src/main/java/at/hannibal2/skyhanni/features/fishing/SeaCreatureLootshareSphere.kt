package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SeaCreatureSettings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LootshareUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereWireframeInWorld

@SkyHanniModule
object SeaCreatureLootshareSphere {
    private val config get() = SkyHanniMod.feature.fishing

    private val seaCreatures = mutableSetOf<LivingSeaCreatureData>()

    private val spherePositions = mutableSetOf<LootshareUtils.Sphere>()

    @HandleEvent
    fun onSeaCreatureSpawn(event: SeaCreatureEvent.Spawn) = addMob(event.seaCreature)

    @HandleEvent
    fun onSeaCreatureRemove(event: SeaCreatureEvent.Remove) = seaCreatures.remove(event.seaCreature)

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lootshareRange) return
        for (entry in spherePositions) {
            event.drawSphereWireframeInWorld(entry.color.toColor(), entry.position, LootshareUtils.RANGE)
        }
        spherePositions.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!config.lootshareRange) return
        for (seaCreature in seaCreatures) {
            if (!seaCreature.exists()) continue
            var otherNearbySpheres = 0
            val pos = seaCreature.pos ?: continue
            val color = if (seaCreature.isOwn || LootshareUtils.isInRange(pos)) LorenzColor.GREEN else LorenzColor.WHITE

            for (lootshareSphere in spherePositions) {
                val position = lootshareSphere.position
                if (position.distance(pos) < 10) otherNearbySpheres++
            }
            if (otherNearbySpheres < 2) {
                spherePositions.add(LootshareUtils.Sphere(pos, color))
            }
        }
    }

    private fun addMob(seaCreature: LivingSeaCreatureData) {
        if (SeaCreatureSettings.getConfig(seaCreature)?.shouldRenderLootshare == true) seaCreatures.add(seaCreature)
    }
}
