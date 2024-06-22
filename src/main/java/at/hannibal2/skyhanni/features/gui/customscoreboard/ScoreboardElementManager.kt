package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getElementFromAny
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Bank
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Bits
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.ChunkedStats
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Cold
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Cookie
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Copper
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Date
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.EmptyLine
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Events
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Extra
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Footer
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Gems
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Heat
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Island
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.LobbyCode
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Location
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Mayor
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Motes
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.NorthStars
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Objective
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Party
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.PlayerAmount
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Powder
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Power
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Profile
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Purse
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Quiver
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.ScoreboardElement
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Slayer
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Soulflow
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Time
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Title
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Tuning
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Visiting
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment

enum class ScoreboardElementManager(val element: ScoreboardElement) {
    TITLE(Title),
    PROFILE(Profile),
    PURSE(Purse),
    MOTES(Motes),
    BANK(Bank),
    BITS(Bits),
    COPPER(Copper),
    GEMS(Gems),
    HEAT(Heat),
    COLD(Cold),
    NORTH_STARS(NorthStars),
    CHUNKED_STATS(ChunkedStats),
    SOULFLOW(Soulflow),
    ISLAND(Island),
    LOCATION(Location),
    PLAYER_AMOUNT(PlayerAmount),
    VISITING(Visiting),
    DATE(Date),
    TIME(Time),
    LOBBY_CODE(LobbyCode),
    POWER(Power),
    TUNING(Tuning),
    COOKIE(Cookie),
    OBJECTIVE(Objective),
    SLAYER(Slayer),
    QUIVER(Quiver),
    POWDER(Powder),
    EVENTS(Events),
    MAYOR(Mayor),
    PARTY(Party),
    FOOTER(Footer),
    EXTRA(Extra),
    EMPTY_LINE(EmptyLine),
    EMPTY_LINE2(EmptyLine),
    EMPTY_LINE3(EmptyLine),
    EMPTY_LINE4(EmptyLine),
    EMPTY_LINE5(EmptyLine),
    EMPTY_LINE6(EmptyLine),
    EMPTY_LINE7(EmptyLine),
    EMPTY_LINE8(EmptyLine),
    EMPTY_LINE9(EmptyLine),
    EMPTY_LINE10(EmptyLine),
    ;

    override fun toString() = element.configLine

    fun getVisiblePair() = if (isVisible()) getPair() else listOf(HIDDEN to HorizontalAlignment.LEFT)

    private fun getPair(): List<ScoreboardElementType> = element.getDisplay().map { getElementFromAny(it) }

    private fun isVisible(): Boolean {
        if (!informationFilteringConfig.hideIrrelevantLines) return true
        return element.showWhen()
    }

    companion object {
        @JvmField
        val defaultOption = listOf(
            TITLE,
            PROFILE,
            PURSE,
            BANK,
            MOTES,
            BITS,
            COPPER,
            NORTH_STARS,
            HEAT,
            COLD,
            EMPTY_LINE,
            ISLAND,
            LOCATION,
            LOBBY_CODE,
            PLAYER_AMOUNT,
            VISITING,
            EMPTY_LINE2,
            DATE,
            TIME,
            EVENTS,
            OBJECTIVE,
            COOKIE,
            EMPTY_LINE3,
            QUIVER,
            POWER,
            TUNING,
            EMPTY_LINE4,
            POWDER,
            MAYOR,
            PARTY,
            FOOTER,
            EXTRA,
        )
    }
}
