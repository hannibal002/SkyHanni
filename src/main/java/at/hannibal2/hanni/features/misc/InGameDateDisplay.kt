package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ScoreboardData
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockTime
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.TimeUtils.formatted
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object InGameDateDisplay {

    private val config get() = HanniMod.feature.gui.inGameDate

    private val patternGroup = RepoPattern.group("misc.ingametime")

    /**
     * REGEX-TEST: Spring 26th
     * REGEX-TEST: Early Winter 1st
     */
    private val monthAndDatePattern by patternGroup.pattern(
        "date",
        ".*(?:(?:Early|Late) )?(?:Winter|Spring|Summer|Autumn) [0-9]{1,2}(?:nd|rd|th|st)?.*",
    )

    /**
     * REGEX-TEST: 8:30am ☀
     * REGEX-TEST: 11:40pm ☽
     */
    private val timeSymbolsPattern by patternGroup.pattern(
        "symbols",
        "[☀☽࿇]",
    )

    private var display = ""

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        if (!config.useScoreboard && !event.repeatSeconds(config.refreshSeconds)) return

        checkDate()
    }

    private fun checkDate() {
        val date = SkyBlockTime.now()
        var theBaseString: String
        if (config.useScoreboard) {
            val list = ScoreboardData.sidebarLinesFormatted // we need this to grab the moon/sun symbol
            val year = "Year ${date.year}"
            var monthAndDate = (list.find { monthAndDatePattern.matches(it) } ?: "??").trim()
            if (monthAndDate.last().isDigit()) {
                monthAndDate = "${monthAndDate}${SkyBlockTime.daySuffix(monthAndDate.takeLast(2).trim().toInt())}"
            }
            val time = list.find { it.lowercase().contains("am ") || it.lowercase().contains("pm ") } ?: "??"
            theBaseString = "$monthAndDate, $year ${time.trim()}".removeColor()
            if (!config.includeSunMoon) {
                theBaseString = timeSymbolsPattern.matcher(theBaseString).replaceAll("")
            }
        } else {
            theBaseString = date.formatted()
            if (config.includeSunMoon) {
                theBaseString = if ((date.hour >= 6) && (date.hour < 17)) "$theBaseString ☀"
                else "$theBaseString ☽"
            }
        }
        if (!config.includeOrdinal) theBaseString = theBaseString.removeOrdinal()
        display = theBaseString
    }

    private fun String.removeOrdinal() = replace("nd", "").replace("rd", "").replace("st", "").replace("th", "")

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.position.renderString(display, posLabel = "In-game Date Display")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
