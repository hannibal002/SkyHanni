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

class OpenContestInElitebotDev {
    /*
    EARLIEST KNOWN CONTEST: https://elitebot.dev/contests/100/6/18
    YEAR 100
    MONTH 6
    DATE 18
    URL STRUCTURE: "https://elitebot.dev/contests/$year/$month/$day"
    UPCOMING CONTESTS: "https://elitebot.dev/contests/upcoming"
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
    val stack = gui.slotUnderMouse?.stack ?: return
    PARENT PATTERN (CHESTNAME AND ITEMDISPLAYNAME):
    ("(?:(?<month>(?:Early |Late )?(?:Winter|Spring|Summer|Autumn|Fall))(?: (?<date>[\d]+)(?:nd|rd|th|st))?, Year (?<year>[\d,.]+))|(?:(?:Jacob's Farming |Your )Contests)") // https://regex101.com/r/BfQfdV/2 -ery
    "YOUR CONTESTS" FIRST LORELINE PATTERN:
    ("((?:§.)+(?<crop>[\S ]+)+ Contests?)") // https://regex101.com/r/7o9eU0/1 -ery
    SB CALENDAR ("MONTH, YEAR") FIRST LORELINE PATTERN:
    ("(?:(?:§.)*(?:[\S ]+)?(?:[\d]+):(?:[\d]+) [ap]m(?:-|[\S ]+)(?:[\d]+):(?:[\d]+) [ap]m: (?:§.)*Jacob's Farming Contest(?:§.)*(?: \((?:§.)*(?:[\d]+[ywhm] )*(?:[\d]+s)(?:§.)*\)| \((?:§.)*(?:[\S ]+)(?:§.)*\))?)") // https://regex101.com/r/1lvgAr/2 -ery
    SB CALENDAR ("MONTH, YEAR") ITEMDISPLAYNAME PATTERN:
    ("Day (?<day>[\d.,]+)") // https://regex101.com/r/0lS3yW/1 -ery
    SB CALENDAR MINECRAFT ITEM VALIDATION: ItemStack(Items.wheat)
    "JACOB'S FARMING CONTESTS" FIRST LORELINE == ("§8Schedule")
     */

    private val config get() = SkyHanniMod.feature.garden.eliteWebsite

    private val EARLIEST_CONTEST_YEAR: Long = 100L
    private val EARLIEST_CONTEST_MONTH: Int = 6
    private val EARLIEST_CONTEST_DATE: Int = 18
    
    private val ELITEBOT_DOMAIN: String = "https://elitebot.dev"
    private val ELITEBOT_CONTESTS: String = "$ELITEBOT_DOMAIN/contests"
    private val ELITEBOT_UPCOMING: String = "$ELITEBOT_CONTESTS/upcoming"

    private val elitebotDevRepoGroup = RepoPattern.group("elitebotdev")

    // private val jacobsFarmingContestChestNamePattern by elitebotDevGroup.pattern(("jacobsfarmingcontest.chestname"), ("(?:(?:Jacob's Farming |Your )Contests)")) // https://regex101.com/r/6dhLMl/1 -ery
    private val calendarDateChestNameItemNamePattern by elitebotDevRepoGroup.pattern(("calendardate.chestnameitemname"), ("(?<sbTime>(?<month>(?:Early |Late )?(?:Winter|Spring|Summer|Autumn|Fall))(?: (?<date>[\\d]+)(?:nd|rd|th|st))?, Year (?<year>[\\d,.]+))")) // https://regex101.com/r/5rZqFd/1 -ery
    private val blankContestsFirstLoreLinePattern by elitebotDevRepoGroup.pattern(("blankcontests.firstloreline"), ("((?:§.)+(?<crop>[\\S ]+)+ Contests?)")) // https://regex101.com/r/7o9eU0/1 -ery
    private val dayBlankItemNamePattern by elitebotDevRepoGroup.pattern(("dayblank.itemname"), ("Day (?<day>[\\d.,]+)")) // https://regex101.com/r/0lS3yW/1 -ery
    private val jacobsFarmingContestSBCalendarFirstLoreLinePattern by elitebotDevRepoGroup.pattern(("jacobsfarmingcontestsbcalendar.firstloreline"), ("(?:(?:§.)*(?:[\\S ]+)?(?:[\\d]+):(?:[\\d]+) [ap]m(?:-|[\\S ]+)(?:[\\d]+):(?:[\\d]+) [ap]m: (?:§.)*Jacob's Farming Contest(?:§.)*(?: \\((?:§.)*(?:[\\d]+[ywhm] )*(?:[\\d]+s)(?:§.)*\\)| \\((?:§.)*(?:[\\S ]+)(?:§.)*\\))?)")) // https://regex101.com/r/1lvgAr/2 -ery

    /*
    {
        id: "minecraft:dye",
        Count: 1b,
        tag: {
            display: {
                Lore: ["§8Cocoa Beans Contest", "", "§7Medal brackets:", "§6§lGOLD §7(§bTop 10%§7): §6618,347", "§f§lSILVER §7(§bTop 30%§7): §6167,058", "§c§lBRONZE §7(§bTop 60%§7): §62,080", "§8§m--------------", "", "§7Your score: §e2,240 collected!", "§7Personal Best: §622,720 collected", "", "§7§7You placed in the §c§lBRONZE §7bracket!", "", "§7Rewards:", "§7§8+§a10x §aJacob's Ticket", "§7§8+§9Turbo-Cocoa I Book", "§7§8+§e1 §7bronze medal", "§8+§b80 Bits", "", "§aReward claimed!"],
                Name: "§aLate Summer 18th, Year 324"
            }
        },
        Damage: 3s
    }

    {
        id: "minecraft:reeds",
        Count: 1b,
        tag: {
            display: {
                Lore: ["§8Sugar Cane Contest", "", "§7Medal brackets:", "§b§lDIAMOND §7(§bTop 2%§7): §6760,162", "§3§lPLATINUM §7(§bTop 5%§7): §6671,288", "§6§lGOLD §7(§bTop 10%§7): §6552,489", "§f§lSILVER §7(§bTop 30%§7): §6225,803", "§c§lBRONZE §7(§bTop 60%§7): §611,295", "§8§m--------------", "", "§7Your score: §e954,963 collected!", "§7Personal Best: §61,066,958 collected", "", "§7§7You placed in the §b§lDIAMOND §7bracket!", "", "§7Rewards:", "§7§8+§a35x §aJacob's Ticket", "§7§8+§9Turbo-Cane I Book", "§7§8+§e1 §7gold medal", "§7§8+§e1 §7silver medal", "§8+§b120 Bits", "", "§aReward claimed!"],
                Name: "§aLate Summer 15th, Year 324"
            }
        },
        Damage: 0s
    }
     */
    /*
    chestName == ("Your Contests")
     */

    /*
    {
        id: "minecraft:wheat",
        Count: 30b,
        tag: {
            overrideMeta: 1b,
            ench: [],
            display: {
                Lore: ["§712:00 am-11:59 pm: §6§eJacob's Farming Contest§7 (§e02h 22m 43s§7)", "§6☘ §7Nether Wart", "§e○ §7Potato", "§e○ §7Wheat"],
                Name: "§aDay 30"
            },
            AttributeModifiers: []
        },
        Damage: 0s
    }
     */
    /*
    chestName pattern: (?:(?<month>(?:Early |Late )?(?:Winter|Spring|Summer|Autumn|Fall)), Year (?<year>[\d,.]+))
     */

    /*
    {
        id: "minecraft:clock",
        Count: 1b,
        tag: {
            display: {
                Lore: ["§8Schedule", "", "§eLate Summer 24th", "§e○ §7Cactus", "§e○ §7Mushroom", "§6☘ §7Pumpkin", "", "§eLate Summer 27th", "§e○ §7Cocoa Beans", "§6☘ §7Potato", "§e○ §7Wheat", "", "§7§8View this info in your full", "§8SkyBlock calendar!"],
                Name: "§6Upcoming Contests"
            }
        },
        Damage: 0s
    }
     */
    /*
    chestName == ("Jacob's Farming Contests")
     */

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
                openPastContestAfterSanityCheck(year, month, day, group("sbTime"))
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
                    openPastContestAfterSanityCheck(year, month, day, "$origMonthString $origDayString $origYearString")
                }
            }
        }
    }

    private fun openUpcoming() {
        LorenzUtils.chat("Opening the upcoming contests page on EliteWebsite.")
        OSUtils.openBrowser(ELITEBOT_UPCOMING)
    }
    
    private fun openPastContestAfterSanityCheck(year: Long, month: Int, day: Int, origSBTime: String) {
        if (calendarDateSanityCheck(year, month, day, SkyBlockTime.now())) {
            LorenzUtils.chat("Opening the farming contests page for $origSBTime on EliteWebsite.")
            OSUtils.openBrowser("$ELITEBOT_CONTESTS/$year/$month/$day")
        } else {
            LorenzUtils.chat("There is no farming contests page for $origSBTime on EliteWebsite. Try again with a different farming contest date.")
        }
    }

    private fun calendarDateSanityCheck(year: Long, month: Int, day: Int, currentSBTime: SkyBlockTime): Boolean = ((year in EARLIEST_CONTEST_YEAR..currentSBTime.year) && (month in EARLIEST_CONTEST_MONTH..currentSBTime.month) && (day in EARLIEST_CONTEST_DATE..currentSBTime.day))

    private fun String.convertMonthNameToInt(): Int = when (this) {
        "Early Spring" -> 1
        "Spring" -> 2
        "Late Spring" -> 3
        "Early Summer" -> 4
        "Summer" -> 5
        "Late Summer" -> 6
        "Early Autumn" -> 7
        "Autumn" -> 8
        "Late Autumn" -> 9
        "Early Fall" -> 7
        "Fall" -> 8
        "Late Fall" -> 9
        "Early Winter" -> 10
        "Winter" -> 11
        "Late Winter" -> 12
        else -> 0
    }

    private fun isEnabled() = config.enabled
}
