package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.Season
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AtmosphericFilterDisplay {

    private val config get() = SkyHanniMod.feature.garden.atmosphericFilterDisplay

    private var display = ""

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && !config.outsideGarden) return
        if (!event.repeatSeconds(1)) return
        display = drawDisplay(Season.getCurrentSeason() ?: return)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.inGarden()) {
            config.position.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        } else {
            config.positionOutside.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        }
    }

    private fun drawDisplay(season: Season): String = buildString {
        if (!config.onlyBuff) {
            append(season.getSeason(config.abbreviateSeason))
            append("§7: ")
        }
        append(season.getPerk(config.abbreviatePerk))
    }

    private fun isEnabled() = config.enabled && (OutsideSbFeature.ATMOSPHERIC_FILTER.isSelected() && !LorenzUtils.inSkyBlock) ||
        (LorenzUtils.inSkyBlock && (GardenAPI.inGarden() || config.outsideGarden))
}
