package at.hannibal2.hanni.features.garden.contest

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.RenderItemTooltipEvent
import at.hannibal2.hanni.features.garden.CropType
import at.hannibal2.hanni.features.garden.FarmingFortuneDisplay.getLatestTrueFarmingFortune
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.features.garden.farming.GardenCropSpeed.getLatestBlocksPerSecond
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.roundTo
import at.hannibal2.hanni.utils.RenderUtils.renderRenderables
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.addLine
import net.minecraft.item.ItemStack
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object JacobContestFFNeededDisplay {

    private val config get() = GardenApi.config.jacobContest
    private var display = emptyList<Renderable>()
    private var lastToolTipTime = SimpleTimeMark.farPast()
    private val cache = mutableMapOf<ItemStack, List<Renderable>>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTooltip(event: RenderItemTooltipEvent) {
        if (!config.ffForContest) return

        if (!FarmingContestApi.inInventory) return
        val stack = event.stack

        val oldData = cache[stack]
        if (oldData != null) {
            display = oldData
            lastToolTipTime = SimpleTimeMark.now()
            return
        }

        val time = FarmingContestApi.getSBTimeFor(stack.displayName) ?: return
        val contest = FarmingContestApi.getContestAtTime(time) ?: return

        val newDisplay = drawDisplay(contest)
        display = newDisplay
        cache[stack] = newDisplay
        lastToolTipTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        cache.clear()
    }

    private fun drawDisplay(contest: FarmingContest) = buildList {
        addString("§6Minimum Farming Fortune needed")
        addString("")

        val crop = contest.crop
        addLine {
            addString("§7For this ")
            addItemStack(crop.icon)
            addString("§7${crop.cropName} contest:")
        }
        for (bracket in ContestBracket.entries) {
            addString(getLine(bracket, contest.brackets, crop))
        }
        addString("")

        val (size, averages) = FarmingContestApi.calculateAverages(crop)
        addLine {
            addString("§7For the last §e$size ")
            addItemStack(crop.icon)
            addString("§7${crop.cropName} contests:")
        }
        for (bracket in ContestBracket.entries) {
            addString(getLine(bracket, averages, crop))
        }
        addString("")

        var blocksPerSecond = crop.getLatestBlocksPerSecond()
        if (blocksPerSecond == null) {
            addLine {
                addString("§cNo ")
                addItemStack(crop.icon)
                addString("§cblocks/second data,")
            }
            addString("§cassuming 19.9 instead.")
        } else {
            if (blocksPerSecond < 15.0) {
                val formatted = blocksPerSecond.roundTo(2)
                addLine {
                    addString("§cYour latest ")
                    addItemStack(crop.icon)
                    addString("§cblocks/second: §e$formatted")
                }
                addString("§cThis is too low, showing 19.9 Blocks/second instead!")
                blocksPerSecond = 19.9
            }
        }
        addString("")

        val trueFF = crop.getLatestTrueFarmingFortune()
        if (trueFF == null) {
            addString("§cNo latest true FF saved!")
        } else {
            val farmingFortune = formatFarmingFortune(trueFF)
            addLine {
                addString("§6Your latest ")
                addItemStack(crop.icon)
                addString("§6FF: $farmingFortune")
            }
        }
        addString("")
        if (blocksPerSecond == null || trueFF == null) {
            addString("§cMissing data from above!")
        } else {
            val predictedScore = ((100.0 + trueFF) * blocksPerSecond * crop.baseDrops * 20 * 60 / 100).toInt().addSeparators()
            addLine {
                addString("§6Predicted ")
                addItemStack(crop.icon)
                addString("§6crops: $predictedScore")
            }
        }
    }

    private fun formatFarmingFortune(farmingFortune: Double) = ceil(farmingFortune).addSeparators()

    private fun getLine(bracket: ContestBracket, map: Map<ContestBracket, Int>, crop: CropType): String {
        val counter = map[bracket] ?: return " ${bracket.displayName}§f: §8Not found!"
        val blocksPerSecond = crop.getRealBlocksPerSecond()
        val cropsPerSecond = counter.toDouble() / blocksPerSecond / 60
        val farmingFortune = (cropsPerSecond * 100 / 20 / crop.baseDrops) - 100
        val format = formatFarmingFortune(farmingFortune.coerceAtLeast(0.0))
        return " ${bracket.displayName}§f: §6$format FF §7(${counter.addSeparators()} crops)"
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.ffForContest) return
        if (!FarmingContestApi.inInventory) return
        if (lastToolTipTime.passedSince() > 200.milliseconds) return
        config.ffForContestPosition.renderRenderables(display, posLabel = "Jacob Contest Crop Data")
    }
}

private fun CropType.getRealBlocksPerSecond(): Double {
    val bps = getLatestBlocksPerSecond() ?: 20.0
    return if (bps < 15.0) {
        return 19.9
    } else bps
}
