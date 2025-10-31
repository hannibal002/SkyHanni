package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.features.inventory.FixIronman
import at.hannibal2.hanni.utils.StringUtils.firstLetterUppercase

// internal and scoreboard
// island change event
object ScoreboardElementProfile : ScoreboardElement() {
    override fun getDisplay() = buildString {
        append(CustomScoreboardUtils.getProfileTypeSymbol())
        if (CustomScoreboard.displayConfig.showProfileName) {
            append(HypixelData.profileName.firstLetterUppercase())
        } else {
            when {
                HypixelData.ironman -> append(FixIronman.getIronmanName())
                HypixelData.stranded -> append("Stranded")
                HypixelData.bingo -> append("Bingo")
                else -> append("Normal")
            }
        }
    }

    override val configLine = "§7♲ Ironman"

    override val elementPatterns = listOf(ScoreboardPattern.profileTypePattern)
}

// click: does a command for profile management exist?
