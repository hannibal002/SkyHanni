package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUtils
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AtmosphericFilterDisplay {

    private val config get() = SkyHanniMod.feature.garden.atmosphericFilterDisplay

    private var display = ""

    private enum class Season(
        val season: String,
        val abbreviatedPerk: String,
        val perk: String,
    ) {

        SPRING("§dSpring", "§6+25☘", "§7Gain §6+25☘ Farming Fortune§7."),
        SUMMER("§6Summer", "§3+20☯", "§7Gain §3+20☯ Farming Wisdom§7."),
        AUTUMN("§eAutumn", "§a15%+§4ൠ", "§4Pests §7spawn §a15% §7more often."),
        WINTER("§9Winter", "§a5%+§cC", "§7Visitors give §a5% §7more §cCopper."),
        ;

        fun getPerk(abbreviate: Boolean) = if (abbreviate) abbreviatedPerk else perk
        fun getSeason(abbreviate: Boolean) = if (abbreviate) season.take(4) else season
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && !config.everywhere) return
        if (!event.repeatSeconds(1)) return
        display = constructPerk(Season.entries.find { it.season.endsWith(TimeUtils.getSeasonByName(SkyBlockTime.now().monthName)) } ?: return)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && config.everywhere) {
            config.posOutside.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        } else if (GardenAPI.inGarden()) {
            config.position.renderString(display, posLabel = "Atmospheric Filter Perk Display")
        }
    }

    private fun constructPerk(season: Season): String = buildString {
        if (!config.onlyBuff) {
            append(season.getSeason(config.abbreviateSeason))
            append("§7: ")
        }
        append(season.getPerk(config.abbreviatePerk))
    }

    private fun isEnabled(): Boolean = config.enabled && LorenzUtils.inSkyBlock
}
