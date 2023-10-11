package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.RenderItemTooltipEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay.Companion.getLatestTrueFarmingFortune
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getLatestBlocksPerSecond
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.ceil

class JacobContestFFNeededDisplay {
    private val config get() = SkyHanniMod.feature.garden
    private var display = emptyList<List<Any>>()
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
        add(listOf("§7For this ", crop.icon, "§7${crop.cropName} contest:"))
        for (bracket in ContestBracket.entries) {
            addAsSingletonList(getLine(bracket, contest.brackets, crop))
        }
        addAsSingletonList("")

        val (size, averages) = FarmingContestAPI.calculateAverages(crop)
        add(listOf("§7For the last §e$size ", crop.icon, "§7${crop.cropName} contests:"))
        for (bracket in ContestBracket.entries) {
            addAsSingletonList(getLine(bracket, averages, crop))
        }
        addAsSingletonList("")

        var blocksPerSecond = crop.getLatestBlocksPerSecond()
        if (blocksPerSecond == null) {
            add(listOf("§cNo ", crop.icon, "§cblocks/second data,"))
            addAsSingletonList("§cassuming 19.9 instead.")
        } else {
            if (blocksPerSecond < 15.0) {
                add(listOf("§7Your latest ", crop.icon, "§7blocks/second: §e${blocksPerSecond.round(2)}"))
                add(listOf("§cThis is too low, showing 19.9 Blocks/second instead!"))
                blocksPerSecond = 19.9
            }
            if (blocksPerSecond < 1) {
                addAsSingletonList("§cLow blocks per second!")
                addAsSingletonList("§cFarm this crop for couple more seconds!")
            }
        }
        addAsSingletonList("")

        val trueFF = crop.getLatestTrueFarmingFortune()
        if (trueFF == null) {
            addAsSingletonList("§cNo latest true FF saved!")
        } else {
            val farmingFortune = formatFarmingFortune(trueFF)
            add(listOf("§6Your ", crop.icon, "§6FF: $farmingFortune"))
        }
        addAsSingletonList("")
        if (blocksPerSecond == null || trueFF == null) {
            add(listOf("§cMissing data from above!"))
        } else {
            val predictedScore = (trueFF * blocksPerSecond * crop.baseDrops * 20 * 60 / 100).toInt().addSeparators()
            add(listOf("§6Predicted ", crop.icon, "§6crops: $predictedScore"))
        }
    }

    private fun formatFarmingFortune(farmingFortune: Double): String {
        var ff = farmingFortune
        if (!config.farmingFortuneDropMultiplier) {
            ff -= 100
            if (ff < 100) {
                ff = 0.0
            }
        }
        return ceil(ff).addSeparators()
    }

    private fun getLine(bracket: ContestBracket, map: Map<ContestBracket, Int>, crop: CropType): String {
        val counter = map[bracket]!!
        val blocksPerSecond = crop.getRealBlocksPerSecond()
        val cropsPerSecond = counter.toDouble() / blocksPerSecond / 60
        val farmingFortune = formatFarmingFortune(cropsPerSecond * 100 / 20 / crop.baseDrops)
        return " ${bracket.displayName}§f: §6$farmingFortune FF §7(${counter.addSeparators()} crops)"
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!FarmingContestAPI.inInventory) return
        if (System.currentTimeMillis() > lastToolTipTime + 200) return
        config.farmingFortuneForContestPos.renderStringsAndItems(display, posLabel = "Jacob Contest Crop Data")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.farmingFortuneForContest
}

private fun CropType.getRealBlocksPerSecond(): Double {
    val bps = getLatestBlocksPerSecond() ?: 20.0
    return if (bps < 15.0) {
        return 19.9
    } else bps
}
