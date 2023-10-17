package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayerSqIgnoreY
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CrystalHollowsNamesInCore {
    val config get() = SkyHanniMod.feature.mining
    private val coreLocations = mapOf(
        LorenzVec(550, 116, 550) to "§8Precursor City",
        LorenzVec(552, 116, 474) to "§bMithril Deposits",
        LorenzVec(477, 116, 476) to "§aJungle",
        LorenzVec(474, 116, 554) to "§6Goblin Hideout"
    )

    var showWaypoints = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (event.isMod(10)) {
            val center = LorenzVec(514.3, 106.0, 514.3)
            showWaypoints = center.distanceToPlayerSqIgnoreY() < 1100 && LocationUtils.playerLocation().y > 65
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        if (showWaypoints) {
            for ((location, name) in coreLocations) {
                event.drawDynamicText(location, name, 2.5)
            }
        }
    }

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isInIsland() && config.crystalHollowsNamesInCore
}