package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AtmosphericFilterDisplay {

    private val config get() = SkyHanniMod.feature.garden.atmosphericFilterDisplay

    private val patternGroup = RepoPattern.group("garden.atmospheric.hud")
    private val seasonPattern by patternGroup.pattern(
        "season.skyblocktime",
        "(?:Early |Late )?(?<season>Spring|Summer|Autumn|Winter)"
    )

    private val posLabel = "Atmospheric Filter Perk Display"

    private var display = ""

    private enum class Seasons(
        val season: String,
        val colorCode: String,
        val abbvPerk: String,
        val perk: String,
    ) {
        SPRING("Spring", "d", "§6+25☘", "§7Gain §6+25☘ Farming Fortune§7."),
        SUMMER("Summer", "6", "§3+20☯", "§7Gain §3+20☯ Farming Wisdom§7."),
        AUTUMN("Autumn", "e", "§a15%+§4ൠ", "§4Pests §7spawn §a15% §7more often."),
        WINTER("Winter", "9", "§a5%+§cC", "§7Visitors give §a5% §7more §cCopper."),
        ;
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && !config.everywhere) return
        if (!event.repeatSeconds(1)) return
        seasonPattern.matchMatcher(SkyBlockTime.now().monthName) {
            display = constructPerk(group("season"))
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && config.everywhere) {
            config.posOutside.renderString(display, posLabel = "$posLabel (Outside Garden)")
        } else if (GardenAPI.inGarden()) {
            config.pos.renderString(display, posLabel = posLabel)
        }
    }

    private fun constructPerk(seasonString: String): String {
        val season = Seasons.values().find {it.season == seasonString} ?: return ""
        if (config.onlyBuff) return if (config.abbreviatePerk) season.abbvPerk else season.perk
        return "§${season.colorCode}${if (config.abbreviateSeason) season.season.take(2) else season.season}§7: ${if (config.abbreviatePerk) season.abbvPerk else season.perk}"
    }

    private fun isEnabled(): Boolean = config.enabled
}
