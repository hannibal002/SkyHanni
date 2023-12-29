package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.data.ClickType
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
import at.hannibal2.skyhanni.utils.NumberUtil.format
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class JacobContestStatsSummary {
    private val config get() = GardenAPI.config.jacobContestStats
    private var blocksBroken = 0
    private var startTime = 0L
    private var startTimeIntoContest = 0.0
    private var percent = 0.0
    private var participants = 0
    private var medalColor = ""
    private var predictedScore = 0L
    private var contestStats = emptyList<List<Any>>()

    private val tabContestPattern = " §r(§e○|§6☘) §r§f(?<crop>.+) §r§f◆ §r§f§lTOP §r(?<color>§.)§l(?<percent>(\\d|[.])+)% §r§f◆ (?<participants>.+) ☻".toPattern()
    private val scoreboardContestTimeLeftPattern = "(§e○|§6☘) §f(?<crop>.+) §a(?<time>\\d+m\\d+s)".toPattern()
    private val scoreboardContestScorePattern = " (Collected|(?<medal>§b§lDIAMOND|§3§lPLATINUM|§6§lGOLD|§f§lSILVER|§c§lBRONZE) §fwith) §e(?<amount>.+)".toPattern()

    @SubscribeEvent
    fun onBlockClick(event: CropClickEvent) {
        if (!isEnabled()) return
        if (!FarmingContestAPI.inContest) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        if (event.crop == FarmingContestAPI.contestCrop) {
            blocksBroken++
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        if (!FarmingContestAPI.inContest) return

        for (line in event.tabList) {
            tabContestPattern.matchMatcher(line) {
                if (group("crop") == FarmingContestAPI.contestCrop?.cropName) {
                    percent = group("percent").toDouble() / 100
                    participants = group("participants").formatNumber().toInt()
                    medalColor = group("color")
                    update()
                }
            }
        }
    }

    @SubscribeEvent
    fun onScoreBoardUpdate(event: ScoreboardChangeEvent) {
        if (!isEnabled()) return
        if (!FarmingContestAPI.inContest) return

        var timeLeft = 0L
        var amount = 0.0

        for (line in event.newList) {
            scoreboardContestTimeLeftPattern.matchMatcher(line) {
                timeLeft = TimeUtils.getDuration(group("time")).inWholeSeconds
            }
            scoreboardContestScorePattern.matchMatcher(line) {
                amount = group("amount").fixScoreAmount()
            }
        }

        predictedScore = ((amount / (1200 - timeLeft - startTimeIntoContest)) * (1200 - startTimeIntoContest)).roundToLong()
        update()
    }

    private fun String.fixScoreAmount(): Double =
        this.formatNumber() * when (this.split(',').last().length) {
            2 -> 10.0
            1 -> 100.0
            else -> 1.0
        }

    fun update() {
        contestStats = emptyList()
        val cropName = FarmingContestAPI.contestCrop?.cropName
        val duration = System.currentTimeMillis() - startTime
        val durationInSeconds = duration.toDouble() / 1000
        val blocksPerSecond = (blocksBroken.toDouble() / durationInSeconds).round(2)
        val position = (percent * participants).roundToInt() + 1
        val formattedPercent = (percent * 100).round(1)

        val unsortedList = buildList<List<Any>> {
            addAsSingletonList("§e§l$cropName Contest Stats")
            addAsSingletonList("§7Started §b${TimeUtils.formatDuration((startTimeIntoContest * 1000 - 999).toLong(), showMilliSeconds = true)} §7into contest")
            addAsSingletonList("§7Blocks Broken: §e${blocksBroken.addSeparators()}")
            addAsSingletonList("§7Blocks per Second: §c$blocksPerSecond")
            if (percent == 0.0 && participants == 0)
                addAsSingletonList("§7Est. Position: §eNo data yet")
            else addAsSingletonList("§7Est. Position: $medalColor$position §7(Top $medalColor$formattedPercent% §7◆ ${format(participants)})")
            addAsSingletonList("§7Predicted Score: §e${predictedScore.addSeparators()}")
        }

        val sortedList = mutableListOf<List<Any>>()
        for (index in config.text) {
            sortedList.add(unsortedList[index.ordinal])
        }
        contestStats = sortedList
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!FarmingContestAPI.inContest) return

        config.pos.renderStringsAndItems(contestStats, posLabel = "Jacob Contest Stats")
    }

    @SubscribeEvent
    fun onFarmingContestEvent(event: FarmingContestEvent) {
        if (!isEnabled()) return

        when (event.phase) {
            FarmingContestPhase.START -> {
                LorenzUtils.chat("Started tracking your Jacob Contest Blocks Per Second!")
                startTime = System.currentTimeMillis()
                startTimeIntoContest = ((startTime.toDouble() % 3600000) - 900000) / 1000
            }

            FarmingContestPhase.STOP -> {
                val cropName = event.crop.cropName
                val duration = System.currentTimeMillis() - startTime
                val durationInSeconds = duration.toDouble() / 1000
                val blocksPerSecond = (blocksBroken.toDouble() / durationInSeconds).round(2)
                val time = TimeUtils.formatDuration(duration - 999)
                val position = (percent * participants).roundToInt() + 1
                val formattedPercent = (percent * 100).round(1)

                LorenzUtils.chat("§l$cropName Contest Stats")
                LorenzUtils.chat("§7Participated for §b$time")
                LorenzUtils.chat("§7Total Blocks Broken: §e${blocksBroken.addSeparators()}")
                LorenzUtils.chat("§7Average Blocks Per Second: §c$blocksPerSecond")
                LorenzUtils.chat("§7Est. Position: $medalColor$position §7(Top $medalColor$formattedPercent% §7◆ ${format(participants)})")
            }

            FarmingContestPhase.CHANGE -> {
                LorenzUtils.chat("You changed the crop during the contest, resetting the Blocks Per Second calculation..")
                startTime = System.currentTimeMillis()
                startTimeIntoContest = ((startTime.toDouble() % 3600000) - 900000) / 1000
            }
        }
        blocksBroken = 0
        percent = 0.0
        participants = 0
    }

    fun isEnabled() = GardenAPI.inGarden() && config.jacobContestSummary
}
