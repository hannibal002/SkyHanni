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

    private val atmosphericFilterDisplayGroup = RepoPattern.group("atmosphericfilter.hud")
    private val seasonPattern by atmosphericFilterDisplayGroup.pattern(
        "season.skyblocktime",
        "(?:(?:Early|Late)? )?(?<season>Spring|Summer|Autumn|Winter)"
    )

    private val posLabel = "Atmospheric Filter Perk Display"

    private var display = ""

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

    private fun constructPerk(season: String): String {
        val perk = when (season) {
            "Spring" -> if (config.abbreviatePerk) "§6+25☘" else "§7Gain §6+25☘ Farming Fortune§7."
            "Summer" -> if (config.abbreviatePerk) "§3+20☯" else "§7Gain §3+20☯ Farming Wisdom§7."
            "Autumn" -> if (config.abbreviatePerk) "§a15%+§4ൠ" else "§4Pests §7spawn §a15% §7more often."
            "Winter" -> if (config.abbreviatePerk) "§a5%+§cC" else "§7Visitors give §a5% §7more §cCopper."
            else -> if (config.abbreviatePerk) "§c?" else "§c??"
        }
        if (config.onlyBuff) return perk
        val colorCode = when (season) {
            "Spring" -> "d"
            "Summer" -> "6"
            "Autumn" -> "e"
            "Winter" -> "9"
            else -> "c"
        }
        return "§$colorCode${if (config.abbreviateSeason) season.take(2) else season}§7: $perk"
    }

    private fun isEnabled(): Boolean = config.enabled
}
