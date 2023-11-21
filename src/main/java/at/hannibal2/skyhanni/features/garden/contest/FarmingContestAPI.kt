package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.FarmingContestEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

object FarmingContestAPI {
    private val timePattern = "§a(?<month>.*) (?<day>.*)(?:rd|st|nd|th), Year (?<year>.*)".toPattern()
    private val contests = mutableMapOf<Long, FarmingContest>()
    private val cropPattern = "§8(?<crop>.*) Contest".toPattern()
    var inContest = false
    var contestCrop: CropType? = null
    private var startTime = SimpleTimeMark.farPast()
    private val sidebarCropPattern = "§e○ §f(?<crop>.*) §a.*".toPattern()

    var inInventory = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (event.repeatSeconds(1)) {
            if (!LorenzUtils.inSkyBlock) return
            if (!GardenAPI.inGarden()) return

            checkActiveContest()
        }
    }

    private fun checkActiveContest() {
        if (inContest && startTime.passedSince() > 20.minutes) {
            FarmingContestEvent(contestCrop!!, FarmingContestPhase.STOP).postAndCatch()
            inContest = false
        }

        val currentCrop = readCurrentCrop()
        val currentContest = currentCrop != null

        if (inContest != currentContest) {
            if (currentContest) {
                FarmingContestEvent(currentCrop!!, FarmingContestPhase.START).postAndCatch()
                startTime = SimpleTimeMark.now()
            } else {
                if (startTime.passedSince() > 2.minutes) {
                    FarmingContestEvent(contestCrop!!, FarmingContestPhase.STOP).postAndCatch()
                }
            }
            inContest = currentContest
        } else {
            if (currentCrop != contestCrop && currentCrop != null) {
                FarmingContestEvent(currentCrop, FarmingContestPhase.CHANGE).postAndCatch()
                startTime = SimpleTimeMark.now()
            }
        }
        contestCrop = currentCrop
    }

    private fun readCurrentCrop(): CropType? {
        var next = false
        for (line in ScoreboardData.sidebarLinesFormatted) {
            if (line == "§eJacob's Contest") {
                next = true
                continue
            }
            if (next) {
                sidebarCropPattern.matchMatcher(line) {
                    return CropType.getByName(group("crop"))
                }
            }
        }

        return null
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName == "Your Contests") {
            inInventory = true
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: GuiContainerEvent.CloseWindowEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    fun getSbTimeFor(text: String) = timePattern.matchMatcher(text) {
        val month = group("month")
        val monthNr = LorenzUtils.getSBMonthByName(month)

        val year = group("year").toInt()
        val day = group("day").toInt()
        SkyBlockTime(year, monthNr, day).toMillis()
    }

    fun addContest(time: Long, item: ItemStack) {
        contests.putIfAbsent(time, createContest(time, item))
    }

    private fun createContest(time: Long, item: ItemStack): FarmingContest {
        val lore = item.getLore()
        val crop = lore.firstNotNullOfOrNull {
            cropPattern.matchMatcher(it) { CropType.getByName(group("crop")) }
        } ?: error("Crop not found in lore!")

        val brackets = buildMap {
            for (bracket in ContestBracket.entries) {
                val amount = lore.firstNotNullOfOrNull {
                    bracket.pattern.matchMatcher(it) {
                        group("amount").replace(",", "").toInt()
                    }
                } ?: continue
                put(bracket, amount)
            }
        }

        return FarmingContest(time, crop, brackets)
    }

    fun getContestAtTime(time: Long) = contests[time]

    fun getContestsOfType(crop: CropType) = contests.values.filter { it.crop == crop }

    fun calculateAverages(crop: CropType): Pair<Int, Map<ContestBracket, Int>> {
        var amount = 0
        val crops = mutableMapOf<ContestBracket, Int>()
        val contests = mutableMapOf<ContestBracket, Int>()
        for (contest in getContestsOfType(crop).associateWith { it.time }.sortedDesc().keys) {
            amount++
            val brackets = contest.brackets
            for ((bracket, count) in brackets) {
                val old = crops.getOrDefault(bracket, 0)
                crops[bracket] = count + old
                contests.addOrPut(bracket, 1)
            }
            if (amount == 10) break
        }
        return Pair(amount, crops.mapValues { (bracket, counter) -> counter / contests[bracket]!! })
    }
}
