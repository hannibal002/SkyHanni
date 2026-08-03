package at.hannibal2.skyhanni.features.foraging.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText

@SkyHanniModule
object SafariNamesInCenter {

    private val config get() = SkyHanniMod.feature.foraging.safari
    private val coreLocations = mapOf(
        LorenzVec(-27.1, 66.0, 22.8) to "§2Forest Biome",
        LorenzVec(-25.5, 66.0, -23.2) to "§5Haunted Biome",
        LorenzVec(-73.3, 65.0, -23.4) to "§9Icy Biome",
        LorenzVec(-72.6, 65.5, 23.9) to "§6Cavern Biome",
    )

    private var showWaypoints = false

    @HandleEvent
    private fun onAreaChange(event: GraphAreaChangeEvent) {
        showWaypoints = event.area == AreaNode.NO_AREA
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() || !showWaypoints) return
        for ((location, name) in coreLocations) {
            if (location.distanceSqToPlayer() > 50) {
                event.drawDynamicText(location, name, 2.5)
            }
        }
    }

    private fun isEnabled() = IslandType.SAFARI.isInIsland() && config.namesInCenter
}
