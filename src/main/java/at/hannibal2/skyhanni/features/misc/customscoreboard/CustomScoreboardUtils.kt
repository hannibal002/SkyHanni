package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft

private val config get() = SkyHanniMod.feature.gui.customScoreboard

object CustomScoreboardUtils {
    fun mayorNameToColorCode(input: String): String {
        return when (input) {
            // Normal Mayors
            "Aatrox" -> "§3$input"
            "Cole" -> "§e$input"
            "Diana" -> "§2$input"
            "Diaz" -> "§6$input"
            "Finnegan" -> "§c$input"
            "Foxy" -> "§d$input"
            "Marina" -> "§b$input"
            "Paul" -> "§c$input"

            // Special Mayors
            "Scorpius" -> "§d$input"
            "Jerry" -> "§d$input"
            "Derpy" -> "§d$input"
            "Dante" -> "§d$input"
            else -> "§cUnknown Mayor: §7$input"
        }
    }

    fun extractLobbyCode(input: String): String? {
        val pattern by RepoPattern.pattern ("features.misc.customscoreboard.lobbycode", "§(\\d{3}/\\d{2}/\\d{2}) §(?<code>.*)$")

        pattern.matchMatcher(input) {
            return group("code")
        }

        return null
    }

    fun getProfileTypeSymbol(): String {
        return when {
            HypixelData.ironman -> "§7♲ " // Ironman
            HypixelData.stranded -> "§a☀ " // Stranded
            HypixelData.bingo -> ScoreboardData.sidebarLines.firstOrNull { it.contains("Bingo") }?.substring(
                0,
                3
            ) + "Ⓑ " // Bingo - gets the first 3 chars of " §9Ⓑ §9Bingo" (you are unable to get the Ⓑ for some reason)
            else -> "§e" // Default case
        }
    }

    fun getTablistFooter(): String {
        val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay
        if (tabList.footer_skyhanni == null) return ""
        return tabList.footer_skyhanni.formattedText.replace("§r", "")
    }

    fun getTitleAndFooterAlignment() = when (config.displayConfig.titleAndFooter.centerTitleAndFooter) {
        true -> AlignmentEnum.CENTER
        false -> AlignmentEnum.LEFT
    }

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
