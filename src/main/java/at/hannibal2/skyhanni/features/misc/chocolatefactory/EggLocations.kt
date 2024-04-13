package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EggLocations {

    private val config get() = SkyHanniMod.feature.misc.chocolateFactory
    // todo need to know when it is the hoppity event

    private var eggLocations: Map<IslandType, List<LorenzVec>> = mapOf()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<HoppityEggLocationsJson>("HoppityEggLocations")

        eggLocations = data.eggLocations
    }

    private fun getCurrentIslandEggLocations(): List<LorenzVec>? {
        return eggLocations[LorenzUtils.skyBlockIsland]
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!config.showAllWaypoints) return

        val eggsLocations = getCurrentIslandEggLocations() ?: return

        if (config.showAllWaypoints) {
            for ((index, eggLocation) in eggsLocations.withIndex()) {
                event.drawWaypointFilled(
                    eggLocation,
                    LorenzColor.GREEN.toColor(),
                    seeThroughBlocks = true,
                    beacon = true
                )
                event.drawDynamicText(eggLocation.add(y = 1), "§aEgg $index", 1.5)
            }
        }
    }
}
