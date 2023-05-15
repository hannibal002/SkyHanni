package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FarmingContestAPI {
    private val timePattern = "§a(?<month>.*) (?<day>.*)(?:rd|st|nd|th), Year (?<year>.*)".toPattern()
    private val contests = mutableMapOf<Long, FarmingContest>()
    private val cropPattern = "§8(?<crop>.*) Contest".toPattern()

    var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
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

        val brackets = ContestBracket.values().associateWith { bracket ->
            lore.firstNotNullOfOrNull {
                bracket.pattern.matchMatcher(it) {
                    group("amount").replace(",", "").toInt()
                }
            } ?: error("Farming contest bracket not found in lore!")
        }

        return FarmingContest(time, crop, brackets)
    }

    fun getContestAtTime(time: Long) = contests[time]

    fun getContestsOfType(crop: CropType) = contests.values.filter { it.crop == crop }

    fun calculateAverages(crop: CropType): Pair<Int, Map<ContestBracket, Int>> {
        var amount = 0
        val map = mutableMapOf<ContestBracket, Int>()
        for (contest in getContestsOfType(crop).associateWith { it.time }.sortedDesc().keys) {
            amount++
            for ((bracket, count) in contest.brackets) {
                val old = map.getOrDefault(bracket, 0)
                map[bracket] = count + old
            }
            if (amount == 10) break
        }
        return Pair(amount, map.mapValues { (_, counter) -> counter / amount })
    }
}