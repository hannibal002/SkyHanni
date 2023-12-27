package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.FarmingContestEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.roundToInt

class JacobContestStatsSummary {
    private val config get() = GardenAPI.config.jacobContestStats
    private var blocksBroken = 0
    private var startTime = 0L
    private var percent = 0.0
    private var participants = 0
    private var contestStats = emptyList<List<Any>>()

    private val tabContestPattern =
        " §r(§e○|§6☘) §r§f(?<crop>.+) §r§f◆ §r§f§lTOP §r§e§l(?<percent>(\\d|[.])+)% §r§f◆ (?<participants>.+) ☻".toPattern()
    private val scoreboardContestTimeLeftPattern = "(§e○|§6☘) §f(?<crop>.+) §a(?<time>\\d+m\\d+s)".toPattern()
    private val scoreboardContestScorePattern = " (Collected|(?<medal>§b§lDIAMOND|§3§lPLATINUM|§6§lGOLD|§f§lSILVER|§c§lBRONZE) §fwith) §e(?<amount>.+)".toPattern()

    @SubscribeEvent
    fun onBlockClick(event: CropClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        if (FarmingContestAPI.inContest && event.crop == FarmingContestAPI.contestCrop) {
            blocksBroken++
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
//         println("enabled: ${isEnabled()} incontest: ${FarmingContestAPI.inContest}")
        if (!isEnabled()) return
        if (!FarmingContestAPI.inContest) return
//         println("tab list updated")
        for (line in event.tabList) {
            tabContestPattern.matchMatcher(line) {
//                 println(group("crop"))
                if (group("crop") == FarmingContestAPI.contestCrop?.cropName) {
                    percent = group("percent").toDouble() / 100
                    participants = group("participants").formatNumber().toInt()

//                     println("percent: $percent participants: $participants")
                }
            }
        }
    }

    @SubscribeEvent
    fun onScoreBoardUpdate(event: ScoreboardChangeEvent) {
        if (!isEnabled()) return

        for (line in event.newList) {
            println(line)
            scoreboardContestTimeLeftPattern.matchMatcher(line) {
                println(group("crop"))
                println(group("time"))
            }
            scoreboardContestScorePattern.matchMatcher("line") {
                println(group("medal"))
                println(group("amount"))
            }
        }
        update()
    }


    fun update() {
        contestStats = emptyList()
        val duration = System.currentTimeMillis() - startTime
        val durationInSeconds = duration.toDouble() / 1000
        val blocksPerSecond = (blocksBroken.toDouble() / durationInSeconds).round(2)
        val cropName = FarmingContestAPI.contestCrop?.cropName
        val time = TimeUtils.formatDuration(duration - 999)
        val position = (percent * participants).roundToInt() + 1

        val unsortedList = buildList<List<Any>> {
            addAsSingletonList("§e§l$cropName Contest Stats")
            addAsSingletonList("§7Participating for §b$time")
            addAsSingletonList("§7Blocks Broken: §e${blocksBroken.addSeparators()}")
            addAsSingletonList("§7Blocks per Second: §c$blocksPerSecond")
            addAsSingletonList("§7Estimated Position: §b$position §7§7(Top §b${percent * 100}% §7◆ $participants)")
            addAsSingletonList("§7Predicted Score: §eidk pattern yet")
        }

        println(unsortedList)
        val sortedList = mutableListOf<List<Any>>()
        for (index in config.text) {
            sortedList.add(unsortedList[index.ordinal])
        }
        println(sortedList)
        contestStats = sortedList
        println(contestStats)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.pos.renderStringsAndItems(contestStats, posLabel = "Jacob Contest Stats")
    }

    @SubscribeEvent
    fun onFarmingContestEvent(event: FarmingContestEvent) {
        if (!isEnabled()) return

        when (event.phase) {
            FarmingContestPhase.START -> {
                LorenzUtils.chat("Started tracking your Jacob Contest Blocks Per Second!")
                startTime = System.currentTimeMillis()
            }

            FarmingContestPhase.STOP -> {
                val duration = System.currentTimeMillis() - startTime
                val durationInSeconds = duration.toDouble() / 1000
                val blocksPerSecond = (blocksBroken.toDouble() / durationInSeconds).round(2)
                val cropName = event.crop.cropName
                LorenzUtils.chat("Stats for $cropName Contest:")
                val time = TimeUtils.formatDuration(duration - 999)
                LorenzUtils.chat("§7Blocks Broken in total: §e${blocksBroken.addSeparators()}")
                val color = getBlocksPerSecondColor(blocksPerSecond)
                LorenzUtils.chat("§7Average Blocks Per Second: $color$blocksPerSecond")
                LorenzUtils.chat("§7Participated for §b$time")
            }

            FarmingContestPhase.CHANGE -> {
                LorenzUtils.chat("You changed the crop during the contest, resetting the Blocks Per Second calculation..")
                startTime = System.currentTimeMillis()
            }
        }
        blocksBroken = 0
    }

    private fun getBlocksPerSecondColor(blocksPerSecond: Double) = if (blocksPerSecond > 19) "§c" else "§a"

    fun isEnabled() = GardenAPI.inGarden() && config.jacobContestSummary
}
