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
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import net.minecraft.world.phys.Vec3

@SkyHanniModule
object CrystalHollowsNamesInCore {

    private val config get() = SkyHanniMod.feature.mining

    private val coreLocations = mapOf(
        Vec3(550.0, 116.0, 550.0) to "§8Precursor Remnants",
        Vec3(552.0, 116.0, 474.0) to "§bMithril Deposits",
        Vec3(477.0, 116.0, 476.0) to "§aJungle",
        Vec3(474.0, 116.0, 554.0) to "§6Goblin Holdout",
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
