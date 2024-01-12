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

    private val elitebotDevRepoGroup = RepoPattern.group("garden.contest.elitebot")
    private val calendarDatePattern by elitebotDevRepoGroup.pattern(
        "date.chestanditem",
        "(?<sbTime>(?<month>(?:Early |Late )?(?:Winter|Spring|Summer|Autumn|Fall))(?: (?<date>\\d+)(?:nd|rd|th|st))?, Year (?<year>[\\d,.]+))"
    )
    private val contestsPattern by elitebotDevRepoGroup.pattern(
        "contests.loreline",
        "((?:§.)+(?<crop>[\\S ]+)+ Contests?)"
    )
    private val dayPattern by elitebotDevRepoGroup.pattern(
        "day.item",
        "Day (?<day>[\\d.,]+)"
    )
    private val jacobsFarmingContestPattern by elitebotDevRepoGroup.pattern(
        "contest.loreline",
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
        val firstLoreLine = item.getLore().first()
        if (itemName == "Upcoming Contests" && chestName == "Jacob's Farming Contests" && firstLoreLine == "§8Schedule") {
            LorenzUtils.chat("§aOpening the upcoming contests page on EliteWebsite.")
            OSUtils.openBrowser(ELITEBOT_UPCOMING)
            return
        }
        val useItemName = chestName == "Your Contests" && contestsPattern.matches(firstLoreLine)
        val useChestName = jacobsFarmingContestPattern.matches(firstLoreLine)
        val theString = if (useItemName) itemName else if (useChestName) chestName else return
        calendarDatePattern.matchMatcher(theString) {
            val origYearString = group("year")
            val origMonthString = group("month")
            var origDayString = group("date") ?: ""
            var sbDate = group("sbTime") ?: ""
            if (useChestName) {
                dayPattern.matchMatcher(itemName) {
                    origDayString = group("day")
                    sbDate = "$origMonthString $origDayString, Year $origYearString"
                }
            }
            openContest(origYearString.formatNumber(), LorenzUtils.getSBMonthByName(origMonthString), origDayString.formatNumber().toInt(), sbDate)
        }
    }

    private fun openContest(yearLong: Long, month: Int, day: Int, sbDate: String) {
        val year = yearLong.toInt()
        if (SkyBlockTime(year, month, day).isValidContest()) {
            LorenzUtils.chat("Opening the contests page for $sbDate.")
            OSUtils.openBrowser("$ELITEBOT_CONTESTS/$year/$month/$day")
        }
    }

    private fun SkyBlockTime.isValidContest(): Boolean = this.asTimeMark() in EARLIEST_CONTEST..SkyBlockTime.now().asTimeMark()
    private fun isEnabled() = config.enabled
}
