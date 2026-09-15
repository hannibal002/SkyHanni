package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText

@SkyHanniModule
object SafariNamesInCenter {

    private val config get() = SkyHanniMod.feature.hunting.safari
    private val areaLocations = mapOf(
        LorenzVec(-27.1, 66.0, 22.8) to SafariBiome.FOREST,
        LorenzVec(-25.5, 66.0, -23.2) to SafariBiome.HAUNTED,
        LorenzVec(-73.3, 65.0, -23.4) to SafariBiome.ICY,
        LorenzVec(-72.6, 65.5, 23.9) to SafariBiome.CAVERN,
    )

    private var showWaypoints = false

    @HandleEvent
    private fun onAreaChange(event: GraphAreaChangeEvent) {
        showWaypoints = event.area == AreaNode.NO_AREA
    }

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.namesInCenter || !showWaypoints) return
        for ((location, biome) in areaLocations) {
            val name = "${biome.formattedName} Biome"

            if (location.distanceSqToPlayer() > 50) {
                event.drawDynamicText(location, name, 2.5)
            }
        }
    }
}
