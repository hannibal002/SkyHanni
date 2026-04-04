package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object AtmosphericFilterDisplay {

    private val config get() = SkyHanniMod.feature.garden.atmosphericFilterDisplay
    private var display: Renderable? = null

    @HandleEvent(onlyOnSkyblockOrFeatures = [OutsideSBFeature.ATMOSPHERIC_FILTER])
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        @Suppress("IsInIslandEarlyReturn")
        if (!GardenApi.inGarden() && !config.outsideGarden) return
        display = drawDisplay(SkyblockSeason.currentSeason ?: return)
    }

    @HandleEvent(onlyOnSkyblockOrFeatures = [OutsideSBFeature.ATMOSPHERIC_FILTER])
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val display = display ?: return
        val position = if (GardenApi.inGarden()) config.position else config.positionOutside
        position.renderRenderable(display, posLabel = "Atmospheric Filter Perk Display")
    }

    private fun drawDisplay(season: SkyblockSeason) = Renderable.text {
        if (!config.onlyBuff) {
            append(season.getSeasonName(config.abbreviateSeason))
            append("§7: ")
        }
        append(season.getPerk(config.abbreviatePerk))
    }

    private fun isEnabled() = SkyBlockUtils.onHypixel && config.enabled && (GardenApi.inGarden() || config.outsideGarden)
}
