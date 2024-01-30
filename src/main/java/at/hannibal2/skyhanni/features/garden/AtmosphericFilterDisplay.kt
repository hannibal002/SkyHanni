package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.Season
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AtmosphericFilterDisplay {

    private val config get() = SkyHanniMod.feature.garden.atmosphericFilterDisplay

    private var display = ""

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && !config.everywhere) return
        if (!event.repeatSeconds(1)) return
        display = drawDisplay(Season.getCurrentSeason() ?: return)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && config.everywhere) {
            config.positionOutside.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        } else if (GardenAPI.inGarden()) {
            config.position.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        }
    }

    private fun drawDisplay(season: Season): String = buildString {
        if (!config.onlyBuff) {
            append(season.getSeason(config.abbreviateSeason))
            append("§7: ")
        }
        append(season.getPerk(config.abbreviatePerk))
    }

    private fun isEnabled(): Boolean = LorenzUtils.inSkyBlock && config.enabled
}
