package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
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

        val ranks = ContestRank.values().associateWith { rank ->
            lore.firstNotNullOfOrNull {
                rank.pattern.matchMatcher(it) {
                    group("amount").replace(",", "").toInt()
                }
            } ?: error("Farming contest rank not found in lore!")
        }

        return FarmingContest(time, crop, ranks)
    }

    fun getContestAtTime(time: Long) = contests[time]

    fun getContestsOfType(crop: CropType) = contests.values.filter { it.crop == crop }
}