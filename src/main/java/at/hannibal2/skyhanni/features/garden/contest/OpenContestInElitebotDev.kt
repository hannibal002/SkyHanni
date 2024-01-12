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
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object OpenContestInElitebotDev {

    private val config get() = SkyHanniMod.feature.garden.eliteWebsite

    private val EARLIEST_CONTEST_YEAR_KNOWN_TO_ELITEWEBSITE: Long = 100L
    private val EARLIEST_CONTEST_MONTH_KNOWN_TO_ELITEWEBSITE: Int = 6
    private val EARLIEST_CONTEST_DATE_KNOWN_TO_ELITEWEBSITE: Int = 18

    private val ELITEBOT_DOMAIN: String = "https://elitebot.dev"
    private val ELITEBOT_CONTESTS: String = "$ELITEBOT_DOMAIN/contests"
    private val ELITEBOT_UPCOMING: String = "$ELITEBOT_CONTESTS/upcoming"
    private val ELITEBOT_RECORDS_SUFFIX: String = "records"

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

    private val SB_MONTH_NAME_LIST: List<String> = listOf<String>(
        "Early Spring",
        "Spring",
        "Late Spring",
        "Early Summer",
        "Summer",
        "Late Summer",
        "Early Autumn",
        "Autumn",
        "Late Autumn",
        "Early Winter",
        "Winter",
        "Late Winter",
    )

    private val elitebotDevRepoGroup = RepoPattern.group("elitebotdev")

    private val calendarDateChestNameItemNamePattern by elitebotDevRepoGroup.pattern(
        ("calendardate.chestnameitemname"),
        ("(?<sbTime>(?<month>(?:Early |Late )?(?:Winter|Spring|Summer|Autumn|Fall))(?: (?<date>[\\d]+)(?:nd|rd|th|st))?, Year (?<year>[\\d,.]+))")
    )
    private val blankContestsFirstLoreLinePattern by elitebotDevRepoGroup.pattern(
        ("blankcontests.firstloreline"),
        ("((?:§.)+(?<crop>[\\S ]+)+ Contests?)")
    )
    private val dayBlankItemNamePattern by elitebotDevRepoGroup.pattern(
        ("dayblank.itemname"),
        ("Day (?<day>[\\d.,]+)")
    )
    private val jacobsFarmingContestSBCalendarFirstLoreLinePattern by elitebotDevRepoGroup.pattern(
        ("jacobsfarmingcontestsbcalendar.firstloreline"),
        ("(?:(?:§.)*(?:[\\S ]+)?(?:[\\d]+):(?:[\\d]+) [ap]m(?:-|[\\S ]+)(?:[\\d]+):(?:[\\d]+) [ap]m: (?:§.)*Jacob's Farming Contest(?:§.)*(?: \\((?:§.)*(?:[\\d]+[ywhm] )*(?:[\\d]+s)(?:§.)*\\)| \\((?:§.)*(?:[\\S ]+)(?:§.)*\\))?)")
    )
    private val calendarDateStringCommandPattern by elitebotDevRepoGroup.pattern(
        ("calendardatestring.command"),
        ("(?<sbTime>(?<month>(?:Early |Late )?(?:Winter|Spring|Summer|Autumn|Fall))?(?: (?<date>[\\d]+)(?:nd|rd|th|st)?)?(?:,? )?Year (?<year>[\\d,.]+))")
    )
    private val calendarDateNumberCommandPattern by elitebotDevRepoGroup.pattern(
        ("calendardatenumber.command"),
        ("(?<one>[\\d]+[ymd]) (?<two>[\\d]+[ymd]) (?<three>[\\d]+[ymd])")
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
            openUpcoming()
        } else if ((chestName == ("Your Contests")) && blankContestsFirstLoreLinePattern.matches(item.getLore().first())) {
            calendarDateChestNameItemNamePattern.matchMatcher(itemName) {
                val year = group("year").formatNumber()
                val month = group("month").convertMonthNameToInt()
                val day = group("date").formatNumber().toInt()
                openYearMonthDay(year, month, day, group("sbTime"))
            }
        } else if (jacobsFarmingContestSBCalendarFirstLoreLinePattern.matches(item.getLore().first())) {
            calendarDateChestNameItemNamePattern.matchMatcher(chestName) {
                val origYearString = group("year")
                val origMonthString = group("month")
                val year = origYearString.formatNumber()
                val month = origMonthString.convertMonthNameToInt()
                dayBlankItemNamePattern.matchMatcher(itemName) {
                    val origDayString = group("day")
                    val day = origDayString.formatNumber().toInt()
                    openYearMonthDay(year, month, day, "$origMonthString $origDayString, Year $origYearString")
                }
            }
        }
    }

    private fun openUpcoming() {
        LorenzUtils.chat("§aOpening the upcoming contests page on EliteWebsite.")
        OSUtils.openBrowser(ELITEBOT_UPCOMING)
    }
    private fun openYearOverview(year: Long) {
        if (calendarDateSanityCheck(year)) {
            LorenzUtils.chat("§aOpening the annual contest records page for §eYear $year §aon EliteWebsite.")
            OSUtils.openBrowser("$ELITEBOT_CONTESTS/$year/$ELITEBOT_RECORDS_SUFFIX")
        } else {
            LorenzUtils.chat("There is no annual contest records page for §aYear $year §eon EliteWebsite.")
        }
    }
    private fun openYearAndMonth(year: Long, month: Int, origMonthString: String) {
        if (calendarDateSanityCheck(year, month)) {
            LorenzUtils.chat("§aOpening §e$origMonthString's §acontest records page for §eYear $year §aon EliteWebsite.")
            OSUtils.openBrowser("$ELITEBOT_CONTESTS/$year/$month")
        } else {
            LorenzUtils.chat("There is no contest records page for §a$origMonthString, Year $year §eon EliteWebsite.")
        }
    }
    private fun openYearMonthDay(year: Long, month: Int, day: Int, origSBTime: String, notFromChest: Boolean = false) {
        val fromChestConditionalString = if (notFromChest) "page for the farming contests closest to" else "farming contests page for"
        if (calendarDateSanityCheck(year, month, day)) {
            LorenzUtils.chat("§aOpening the $fromChestConditionalString $origSBTime on EliteWebsite.")
            OSUtils.openBrowser("$ELITEBOT_CONTESTS/$year/$month/$day")
        } else {
            LorenzUtils.chat("There is no farming contests page for §a$origSBTime §eon EliteWebsite. Try again with a different farming contest date.")
        }
    }

    private fun sendUsageMessagesCalendarDate() {
        LorenzUtils.chat("§cUsage: /shopencontest §b[case-sensitive month name] §b[day] §cYear <year number>")
        LorenzUtils.chat("Parameters colored like §bthis §eare optional.")
    }

    private fun sendUsageMessagesNumbers(argsAsOneString: String = "") {
        LorenzUtils.chat("§cUsage example: /shelitebotdevcontest <month number>m <day number>d <year number>y")
        LorenzUtils.chat("All parameters are required, but they can be entered in any order (as long as you include the correct suffix).")
        if (argsAsOneString == "") return
        LorenzUtils.chat("You entered: $argsAsOneString")
    }

    fun openFromCommandString(args: Array<String>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) {
            LorenzUtils.chat("You have disabled opening past farming contests on EliteWebsite. Visit your config to enable this.")
            return
        }
        if (args.isEmpty()) {
            sendUsageMessagesCalendarDate()
            return
        }
        val calendarDateString = args.joinToString(" ")
        if (calendarDateStringCommandPattern.matches(calendarDateString)) {
            calendarDateStringCommandPattern.matchMatcher(calendarDateString) {
                val sbTime = group("sbTime") ?: ""
                val yearString = group("year") ?: ""
                val monthString = group("month") ?: ""
                val dayString = group("date") ?: ""
                if (sbTime.isEmpty() || yearString.isEmpty() || !(calendarDateString.contains("Year"))) {
                    sendUsageMessagesCalendarDate()
                    return
                } else if (dayString.isEmpty() && monthString.isEmpty() && yearString.isNotEmpty()) {
                    openYearOverview(yearString.formatNumber())
                    return
                } else if (dayString.isEmpty() && monthString.isNotEmpty() && yearString.isNotEmpty()) {
                    openYearAndMonth(yearString.formatNumber(), monthString.convertMonthNameToInt(), monthString)
                } else if (dayString.isNotEmpty() && monthString.isNotEmpty() && yearString.isNotEmpty()) {
                    openYearMonthDay(yearString.formatNumber(), monthString.convertMonthNameToInt(), dayString.formatNumber().toInt(), calendarDateString, true)
                } else {
                    LorenzUtils.chat("§cIf you're reading this inside Minecraft, something went wrong with parsing your calendar date string. Please copy your original input below and report this bug on the SkyHanni Discord server.")
                    LorenzUtils.chat(calendarDateString)
                }
            }
        } else {
            LorenzUtils.chat("You entered $calendarDateString, which could not be read correctly.")
            sendUsageMessagesCalendarDate()
        }
    }

    fun openFromCommandNumbers(args: Array<String>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) {
            LorenzUtils.chat("You have disabled opening past farming contests on EliteWebsite. Visit your config to enable this.")
            return
        }
        if (args.isEmpty()) {
            sendUsageMessagesNumbers()
            return
        }
        val argsAsOneString = args.joinToString(" ")
        if (args.size != 3) {
            sendUsageMessagesNumbers(argsAsOneString)
            return
        }
        val mapOfMatches: MutableMap<String, Int> = mutableMapOf(
            "month" to 0,
            "day" to 0,
            "year" to 0,
        )
        if (calendarDateNumberCommandPattern.matches(argsAsOneString)) {
            calendarDateNumberCommandPattern.matchMatcher(argsAsOneString) {
                val timeUnitOne = group("one") ?: ""
                val timeUnitTwo = group("two") ?: ""
                val timeUnitThree = group("three") ?: ""
                val timeUnitsStrings: List<String> = listOf<String>(timeUnitOne, timeUnitTwo, timeUnitThree)
                if (timeUnitsStrings.any { it.isEmpty() }) {
                    sendUsageMessagesNumbers(argsAsOneString)
                    return
                }
                val timeUnitsInts: MutableList<Int> = mutableListOf(0, 0, 0)
                for (timeUnit in timeUnitsStrings) {
                    val lastLetter = timeUnit.takeLast(1)
                    if (lastLetter == "y") {
                        mapOfMatches["year"] = mapOfMatches.getOrDefault("year", 0) + 1
                        timeUnitsInts[0] = timeUnit.removeSuffix(lastLetter).toInt()
                    } else if (lastLetter == "m") {
                        mapOfMatches["month"] = mapOfMatches.getOrDefault("month", 0) + 1
                        timeUnitsInts[1] = timeUnit.removeSuffix(lastLetter).toInt()
                    } else if (lastLetter == "d") {
                        mapOfMatches["day"] = mapOfMatches.getOrDefault("day", 0) + 1
                        timeUnitsInts[2] = timeUnit.removeSuffix(lastLetter).toInt()
                    }
                }
                if (mapOfMatches.any { it.value != 1 }) {
                    sendUsageMessagesNumbers(argsAsOneString)
                    return
                }
                openYearMonthDay(year = timeUnitsInts[0].toLong(), month = timeUnitsInts[1], day = timeUnitsInts[2], "${timeUnitsInts[1].convertIntToMonthName()} ${timeUnitsInts[2]}, Year ${timeUnitsInts[0]}", true)
            }
        } else {
            sendUsageMessagesNumbers(argsAsOneString)
            return
        }
    }

    private fun calendarDateSanityCheck(year: Long, month: Int = 1, day: Int = EARLIEST_CONTEST_DATE_KNOWN_TO_ELITEWEBSITE, currentSBTime: SkyBlockTime = SkyBlockTime.now()): Boolean {
        if (failsYearSanityCheck(year, currentSBTime.year)) return false
        if (year == EARLIEST_CONTEST_YEAR_KNOWN_TO_ELITEWEBSITE && failsMonthSanityCheckEarliestContest(month)) return false
        if (year == EARLIEST_CONTEST_YEAR_KNOWN_TO_ELITEWEBSITE && month == EARLIEST_CONTEST_MONTH_KNOWN_TO_ELITEWEBSITE && failsDaySanityCheckEarliestContest(day)) return false
        if (year == currentSBTime.year.toLong() && failsMonthSanityCheck(month, currentSBTime.month)) return false
        if (year == currentSBTime.year.toLong() && month == currentSBTime.month && failsDaySanityCheck(day, currentSBTime.day)) return false
        return true
    }
    private fun failsYearSanityCheck(year: Long, currentSBTimeYear: Int): Boolean = year !in EARLIEST_CONTEST_YEAR_KNOWN_TO_ELITEWEBSITE..currentSBTimeYear
    private fun failsMonthSanityCheck(month: Int, currentSBTimeMonth: Int): Boolean = month !in 1..currentSBTimeMonth
    private fun failsDaySanityCheck(day: Int, currentSBTimeDay: Int) = day !in 1..currentSBTimeDay
    private fun failsMonthSanityCheckEarliestContest(month: Int): Boolean = month !in EARLIEST_CONTEST_MONTH_KNOWN_TO_ELITEWEBSITE..12
    private fun failsDaySanityCheckEarliestContest(day: Int): Boolean = day !in EARLIEST_CONTEST_DATE_KNOWN_TO_ELITEWEBSITE..30
    private fun String.convertMonthNameToInt(): Int = SB_MONTH_NAME_INT_MAP.getOrElse(this) { LorenzUtils.chat("§c\"$this\" is not a valid month name. Defaulting to \"Early Spring\"."); 1 }
    private fun Int.convertIntToMonthName(): String = SB_MONTH_NAME_LIST[this - 1]
    private fun isEnabled() = config.enabled
}
