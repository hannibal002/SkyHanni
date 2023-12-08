package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import net.minecraft.client.Minecraft

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
        val regex = Regex("§(\\d{3}/\\d{2}/\\d{2}) §([A-Za-z0-9]+)$")
        val matchResult = regex.find(input)
        return matchResult?.groupValues?.lastOrNull()
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

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
