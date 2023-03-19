package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderSingleLineWithItems
import at.hannibal2.skyhanni.utils.TimeUtils
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.time.Instant
import java.util.regex.Pattern

class GardenNextJacobContest {
    private val display = mutableListOf<Any>()
    private var tick = 0
    private var contests = mutableMapOf<Long, FarmingContest>()
    private var inCalendar = false
    private val patternDay = Pattern.compile("§aDay (.*)")
    private val patternMonth = Pattern.compile("(.*), Year (.*)")
    private val patternCrop = Pattern.compile("§e○ §7(.*)")

    private val maxContestsPerYear = 124
    private val contestDuration = 1_000 * 60 * 20

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (tick++ % (40) != 0) return

        if (inCalendar) return
        update()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (inCalendar) {
            inCalendar = false
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!config.nextJacobContestDisplay) return

        val backItem = event.inventoryItems[48] ?: return
        val backName = backItem.name
        if (backName != "§aGo Back") return
        val lore = backItem.getLore()
        if (lore.size != 1) return
        if (lore[0] != "§7To Calendar and Events") return

        inCalendar = true
        readCalendar(event)
    }

    private fun readCalendar(event: InventoryOpenEvent) {
        val inventoryName = event.inventoryName

        val matcher = patternMonth.matcher(inventoryName)
        if (!matcher.matches()) return
        val month = LorenzUtils.getSBMonthByName(matcher.group(1))
        val year = matcher.group(2).toInt()

        if (contests.isNotEmpty()) {
            val contest = contests.values.first()
            val endTime = contest.endTime
            val lastYear = SkyBlockTime.fromInstant(Instant.ofEpochMilli(endTime)).year
            if (year != lastYear) {
                contests.clear()
                LorenzUtils.chat("§e[SkyHanni] New year detected, open all calendar months again!")
            }
        }

        for (item in event.inventoryItems.values) {
            val lore = item.getLore()
            if (!lore.any { it.contains("§6§eJacob's Farming Contest") }) continue

            val name = item.name ?: continue
            val matcherDay = patternDay.matcher(name)
            if (!matcherDay.matches()) continue

            val day = matcherDay.group(1).toInt()
            val startTime = SkyBlockTime(year, month, day).toMillis()
            val crops = mutableListOf<String>()
            for (line in lore) {
                val matcherCrop = patternCrop.matcher(line)
                if (!matcherCrop.matches()) continue
                crops.add(matcherCrop.group(1))
            }
            val contest = FarmingContest(startTime + contestDuration, crops)
            contests[startTime] = contest
        }

        update()
        saveConfig()
    }

    private fun saveConfig() {
        val map = SkyHanniMod.feature.hidden.gardenJacobFarmingContestTimes
        map.clear()
        for (contest in contests.values) {
            map[contest.endTime] = contest.crops
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for ((time, crops) in SkyHanniMod.feature.hidden.gardenJacobFarmingContestTimes) {
            contests[time] = FarmingContest(time, crops)
        }
    }

    class FarmingContest(val endTime: Long, val crops: List<String>)

    private fun update() {
        nextContestCrops.clear()
        val newDisplay = drawDisplay()
        display.clear()
        display.addAll(newDisplay)
    }

    private fun drawDisplay(): List<Any> {
        val list = mutableListOf<Any>()

        if (inCalendar) {
            val size = contests.size
            val percentage = size.toDouble() / maxContestsPerYear
            val formatted = LorenzUtils.formatPercentage(percentage)
            list.add("§eDetected $formatted of farming contests this year")

            return list
        }

        if (contests.isEmpty()) {
            list.add("§cOpen calendar to read jacob contest times!")
            return list
        }

        val nextContest =
            contests.filter { it.value.endTime > System.currentTimeMillis() }.toSortedMap()
                .firstNotNullOfOrNull { it.value }
        if (nextContest == null) {
            if (contests.size == maxContestsPerYear) {
                list.add("§cNew SkyBlock Year! Open calendar again!")
            } else {
                list.add("§cOpen calendar to read jacob contest times!")
            }
            return list
        }

        return drawNextContest(nextContest, list)
    }

    private fun drawNextContest(
        nextContest: FarmingContest,
        list: MutableList<Any>,
    ): MutableList<Any> {
        var duration = nextContest.endTime - System.currentTimeMillis()
        if (duration < contestDuration) {
            list.add("§aActive: ")
        } else {
            list.add("§eNext: ")
            duration -= contestDuration
        }
        for (crop in nextContest.crops) {
            list.add(" ")
            GardenAPI.addGardenCropToList(crop, list)
            nextContestCrops.add(crop)
        }
        val format = TimeUtils.formatDuration(duration)
        list.add("§7(§b$format§7)")

        return list
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.nextJacobContestPos.renderSingleLineWithItems(display, 1.7)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!config.nextJacobContestDisplay) return
        if (!inCalendar) return

        config.nextJacobContestPos.renderSingleLineWithItems(display)
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.nextJacobContestDisplay
            && (GardenAPI.inGarden() || config.nextJacobContestEverywhere)

    companion object {
        private val config get() = SkyHanniMod.feature.garden
        private val nextContestCrops = mutableListOf<String>()

        fun isNextCrop(cropName: String) = nextContestCrops.contains(cropName) && config.nextJacobContestOtherGuis
    }
}