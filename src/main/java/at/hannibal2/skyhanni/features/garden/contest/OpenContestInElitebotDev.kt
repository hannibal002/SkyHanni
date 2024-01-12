package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object OpenContestInElitebotDev {

    private val config get() = SkyHanniMod.feature.garden.eliteWebsite

    private val EARLIEST_CONTEST: SimpleTimeMark = SkyBlockTime(year = 100, month = 6, day = 18).asTimeMark()

    private const val ELITEBOT_DOMAIN: String = "https://elitebot.dev"
    private const val ELITEBOT_CONTESTS: String = "$ELITEBOT_DOMAIN/contests"
    private const val ELITEBOT_UPCOMING: String = "$ELITEBOT_CONTESTS/upcoming"
    private const val ELITEBOT_RECORDS_SUFFIX: String = "records"

    private val SB_MONTH_NAME_INT_MAP: Map<String, Int> = mapOf(
        "Early Spring" to 1,
        "Spring" to 2,
        "Late Spring" to 3,
        "Early Summer" to 4,
        "Summer" to 5,
        "Late Summer" to 6,
        "Early Autumn" to 7,
        "Autumn" to 8,
        "Late Autumn" to 9,
        "Early Fall" to 7,
        "Fall" to 8,
        "Late Fall" to 9,
        "Early Winter" to 10,
        "Winter" to 11,
        "Late Winter" to 12,
    )

    private val elitebotDevRepoGroup = RepoPattern.group("elitebotdev")

    private val calendarDateChestNameItemNamePattern by elitebotDevRepoGroup.pattern(
        "calendardate.chestnameitemname",
        "(?<sbTime>(?<month>(?:Early |Late )?(?:Winter|Spring|Summer|Autumn|Fall))(?: (?<date>\\d+)(?:nd|rd|th|st))?, Year (?<year>[\\d,.]+))"
    )
    private val blankContestsFirstLoreLinePattern by elitebotDevRepoGroup.pattern(
        "blankcontests.firstloreline",
        "((?:§.)+(?<crop>[\\S ]+)+ Contests?)"
    )
    private val dayBlankItemNamePattern by elitebotDevRepoGroup.pattern(
        "dayblank.itemname",
        "Day (?<day>[\\d.,]+)"
    )
    private val jacobsFarmingContestSBCalendarFirstLoreLinePattern by elitebotDevRepoGroup.pattern(
        "jacobsfarmingcontestsbcalendar.firstloreline",
        "(?:§.)*(?:[\\S ]+)?\\d+:\\d+ [ap]m(?:-|[\\S ]+)\\d+:\\d+ [ap]m: (?:§.)*Jacob's Farming Contest(?:§.)*(?: \\((?:§.)*(?:\\d+[ywhm] )*\\d+s(?:§.)*\\)| \\((?:§.)*[\\S ]+(?:§.)*\\))?"
    )

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (!config.eliteWebsiteKeybind.isKeyHeld()) return
        if (event.gui !is GuiChest) return
        val item = event.slot?.stack ?: return
        val chestName = InventoryUtils.openInventoryName()
        val itemName = item.cleanName()
        if ((itemName == ("Upcoming Contests")) && (chestName == ("Jacob's Farming Contests")) && (item.getLore().first() == ("§8Schedule"))) {
            LorenzUtils.chat("§aOpening the upcoming contests page on EliteWebsite.")
            OSUtils.openBrowser(ELITEBOT_UPCOMING)
        } else if ((chestName == ("Your Contests")) && blankContestsFirstLoreLinePattern.matches(item.getLore().first())) {
            calendarDateChestNameItemNamePattern.matchMatcher(itemName) {
                openContest(group("year").formatNumber(), group("month").convertMonthNameToInt(), group("date").formatNumber().toInt(), group("sbTime"))
            }
        } else if (jacobsFarmingContestSBCalendarFirstLoreLinePattern.matches(item.getLore().first())) {
            calendarDateChestNameItemNamePattern.matchMatcher(chestName) {
                val origYearString = group("year")
                val origMonthString = group("month")
                dayBlankItemNamePattern.matchMatcher(itemName) {
                    val origDayString = group("day")
                    openContest(origYearString.formatNumber(), origMonthString.convertMonthNameToInt(), origDayString.formatNumber().toInt(), "$origMonthString $origDayString, Year $origYearString")
                }
            }
        }
    }

    private fun openContest(yearLong: Long, month: Int = -1, day: Int = -1, sbDate: String, fromCommand: Boolean = false) {
        val year = yearLong.toInt()
        if (month == -1 && day == -1 && SkyBlockTime(year).passesCalendarDateSanityCheck()) {
            LorenzUtils.chat("Opening the year-specfic contests page for $sbDate.")
            OSUtils.openBrowser("$ELITEBOT_CONTESTS/$year/$ELITEBOT_RECORDS_SUFFIX")
        } else if (day == -1 && SkyBlockTime(year, month).passesCalendarDateSanityCheck()) {
            LorenzUtils.chat("Opening the contests page for $sbDate.")
            OSUtils.openBrowser("$$ELITEBOT_CONTESTS/$year/$month")
        } else if (SkyBlockTime(year, month, day).passesCalendarDateSanityCheck()) {
            val onExactDate = if (fromCommand) "nearest to" else "for"
            LorenzUtils.chat("Opening the contests page $onExactDate $sbDate.")
            OSUtils.openBrowser("$$ELITEBOT_CONTESTS/$year/$month/$day")
        }
    }

    private fun SkyBlockTime.passesCalendarDateSanityCheck(): Boolean = this.asTimeMark() >= EARLIEST_CONTEST
    private fun String.convertMonthNameToInt(): Int = SB_MONTH_NAME_INT_MAP.getOrElse(this) { LorenzUtils.chat("§c\"$this\" is not a valid month name. Defaulting to \"Early Spring\"."); 1 }
    private fun isEnabled() = config.enabled
}
