package at.hannibal2.hanni.features.mining.crystalhollows

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LocationUtils
import at.hannibal2.hanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText

@HanniModule
object CrystalHollowsNamesInCore {

    private val config get() = HanniMod.feature.mining
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
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled() || !showWaypoints) return
        for ((location, name) in coreLocations) {
            if (location.distanceSqToPlayer() > 50) {
                event.drawDynamicText(location, name, 2.5)
            }
        }
    }

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isCurrent() && config.crystalHollowsNamesInCore
}
