package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText

@SkyHanniModule
object CrystalHollowsNamesInCore {

    private val config get() = SkyHanniMod.feature.mining
    private val coreLocations = mapOf(
        LorenzVec(550, 116, 550) to "§8Precursor Remnants",
        LorenzVec(552, 116, 474) to "§bMithril Deposits",
        LorenzVec(477, 116, 476) to "§aJungle",
        LorenzVec(474, 116, 554) to "§6Goblin Holdout",
    )

    private var showWaypoints = false
    private var inNucleus = false

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inNucleus = event.area == "Crystal Nucleus"
        update()
    }

    private fun update() {
        showWaypoints = inNucleus && LocationUtils.playerLocation().y > 65
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (isEnabled()) update()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() || !showWaypoints) return
        for ((location, name) in coreLocations) {
            if (location.distanceSqToPlayer() > 50) {
                event.drawDynamicText(location, name, 2.5)
            }
        }
    }

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isCurrent() && config.crystalHollowsNamesInCore
}
