package at.hannibal2.hanni.features.garden

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.SkyblockSeason

@HanniModule
object AtmosphericFilterDisplay {

    private val config get() = HanniMod.feature.garden.atmosphericFilterDisplay

    private var display = ""

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        @Suppress("IsInIslandEarlyReturn")
        if (!GardenApi.inGarden() && !config.outsideGarden) return
        display = drawDisplay(SkyblockSeason.currentSeason ?: return)
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenApi.inGarden()) {
            config.position.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        } else {
            config.positionOutside.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        }
    }

    private fun drawDisplay(season: SkyblockSeason): String = buildString {
        if (!config.onlyBuff) {
            append(season.getSeasonName(config.abbreviateSeason))
            append("§7: ")
        }
        append(season.getPerk(config.abbreviatePerk))
    }

    private fun isEnabled() = SkyBlockUtils.onHypixel && config.enabled && (
        (OutsideSBFeature.ATMOSPHERIC_FILTER.isSelected() && !SkyBlockUtils.inSkyBlock) ||
            (SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.outsideGarden))
        )
}
