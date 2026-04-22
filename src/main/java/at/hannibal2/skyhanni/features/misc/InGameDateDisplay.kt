package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object InGameDateDisplay {

    private val config get() = SkyHanniMod.feature.gui.inGameDate

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

    private var display: Renderable? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) = with(config) {
        if (!enabled || (!useScoreboard && !event.repeatSeconds(refreshSeconds))) return
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
        display = Renderable.text(theBaseString)
    }

    private fun String.removeOrdinal() = replace("nd", "").replace("rd", "").replace("st", "").replace("th", "")

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return

        val display = display ?: return
        config.position.renderRenderable(display, posLabel = "In-game Date Display")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
