package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.RenderItemTooltipEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.ceil

class JacobContestFFNeededDisplay {
    private val config get() = SkyHanniMod.feature.garden
    private var display = listOf<List<Any>>()
    private var lastToolTipTime = 0L
    private val cache = mutableMapOf<ItemStack, List<List<Any>>>()

    @SubscribeEvent
    fun onTooltip(event: RenderItemTooltipEvent) {
        if (!isEnabled()) return

        if (!InventoryUtils.openInventoryName().contains("Your Contests")) return
        val stack = event.stack

        val oldData = cache[stack]
        if (oldData != null) {
            display = oldData
            lastToolTipTime = System.currentTimeMillis()
            return
        }

        val name = stack.name ?: return
        val time = FarmingContestAPI.getSbTimeFor(name) ?: return
        val contest = FarmingContestAPI.getContestAtTime(time) ?: return

        val newDisplay = drawDisplay(contest)
        display = newDisplay
        cache[stack] = newDisplay
        lastToolTipTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        cache.clear()
    }

    private fun drawDisplay(contest: FarmingContest) = buildList<List<Any>> {
        addAsSingletonList("§6Minimum Farming Fortune needed")
        addAsSingletonList("")

        val crop = contest.crop
        add(listOf("For this ", crop.icon, "${crop.cropName} contest:"))
        for (rank in ContestRank.values()) {
            addAsSingletonList(getLine(rank, contest.ranks, crop))
        }
        addAsSingletonList("")

        val (size, averages) = calculateAverages(crop)
        add(listOf("For the last $size ", crop.icon, "${crop.cropName} contests:"))
        for (rank in ContestRank.values()) {
            addAsSingletonList(getLine(rank, averages, crop))
        }
    }

    private fun getLine(rank: ContestRank, map: Map<ContestRank, Int>, crop: CropType): String {
        val counter = map[rank]!!
        val cropsPerSecond = counter.toDouble() / 20 / 60
        // allows for crop specific block/second data from the user in the future either through config or through the existing block/second function
        val blocksPerSecond = if (crop.cropName == "Cactus" && !config.cactusAboveSpeedLimit) 17 else config.farmingBlocksBrokenPerSecond
        var farmingFortune = ceil(cropsPerSecond * 100 / blocksPerSecond.toDouble() / crop.baseDrops)
        if (!config.farmingFortuneDropMultiplier)  farmingFortune -= 100
        return " ${rank.displayName}§f: §6${farmingFortune.addSeparators()} FF §7(${counter.addSeparators()} crops)"
    }

    private fun calculateAverages(crop: CropType): Pair<Int, Map<ContestRank, Int>> {
        var amount = 0
        val map = mutableMapOf<ContestRank, Int>()
        for (entry in FarmingContestAPI.getContestsOfType(crop).associateWith { it.time }.sortedDesc().keys) {
            amount++
            for ((rank, count) in entry.ranks) {
                val old = map.getOrDefault(rank, 0)
                map[rank] = count + old
            }
            if (amount == 10) break
        }
        return Pair(amount, map.mapValues { (_, counter) -> counter / amount })
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!isEnabled()) return
        if (!FarmingContestAPI.inInventory) return
        if (System.currentTimeMillis() > lastToolTipTime + 200) return
        config.farmingFortuneForContestPos.renderStringsAndItems(display, posLabel = "Estimated Item Value")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.farmingFortuneForContest
}