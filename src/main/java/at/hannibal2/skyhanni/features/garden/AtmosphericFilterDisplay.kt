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

    val config get() = SkyHanniMod.feature.garden.atmosphericFilterDisplay

    private val patternGroup = RepoPattern.group("garden.atmospheric.hud")
    private val seasonPattern by patternGroup.pattern(
        "season.skyblocktime",
        "(?:Early |Late )?(?<season>Spring|Summer|Autumn|Winter)"
    )

    private val posLabel = "Atmospheric Filter Perk Display"
    private val posLabelOutsideGarden = "$posLabel (Outside Garden)"

    private var display = ""
    private var currentSeason: Seasons? = null

    private enum class Seasons(
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
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && !config.everywhere) return
        if (!event.repeatSeconds(1)) return
        seasonPattern.matchMatcher(SkyBlockTime.now().monthName) {
            val readValue = Seasons.entries.find { it.season.endsWith(group("season")) } ?: return
            if (currentSeason == readValue) return
            currentSeason = readValue
            display = constructPerk(readValue)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (!GardenAPI.inGarden() && config.everywhere) {
            config.posOutside.renderString(display, posLabel = posLabelOutsideGarden)
        } else if (GardenAPI.inGarden()) {
            config.pos.renderString(display, posLabel = posLabel)
        }
    }

    private fun constructPerk(season: Seasons): String = buildString {
        if (!config.onlyBuff) {
            append(season.getSeason(config.abbreviateSeason))
            append("§7: ")
        }
        append(season.getPerk(config.abbreviatePerk))
    }

    private fun isEnabled(): Boolean = config.enabled
}
