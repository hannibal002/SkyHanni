package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SkyblockSeason
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object AtmosphericFilterDisplay {

    private val config get() = SkyHanniMod.feature.garden.atmosphericFilterDisplay

    private var display = ""

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && !config.outsideGarden) return
        display = drawDisplay(SkyblockSeason.currentSeason ?: return)
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

    private fun drawDisplay(season: SkyblockSeason): String = buildString {
        if (!config.onlyBuff) {
            append(season.getSeason(config.abbreviateSeason))
            append("§7: ")
        }
        append(season.getPerk(config.abbreviatePerk))
    }

    private fun isEnabled() = LorenzUtils.onHypixel && config.enabled && (
        (OutsideSbFeature.ATMOSPHERIC_FILTER.isSelected() && !LorenzUtils.inSkyBlock) ||
            (LorenzUtils.inSkyBlock && (GardenAPI.inGarden() || config.outsideGarden))
        )
}
