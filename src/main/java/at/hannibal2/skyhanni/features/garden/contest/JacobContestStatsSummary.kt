package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.FarmingContestEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class JacobContestStatsSummary {
    private val config get() = SkyHanniMod.feature.garden
    private var blocksBroken = 0
    private var startTime = 0L

    @SubscribeEvent
    fun onBlockClick(event: CropClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        if (FarmingContestAPI.inContest && event.crop == FarmingContestAPI.contestCrop) {
            blocksBroken++
        }
    }

    @SubscribeEvent
    fun onFarmingContestEvent(event: FarmingContestEvent) {
        if (!isEnabled()) return

        when (event.phase) {
            FarmingContestPhase.START -> {
                LorenzUtils.chat("§e[SkyHanni] Started tracking your Jacob Contest Blocks Per Second!")
                startTime = System.currentTimeMillis()
            }
            FarmingContestPhase.STOP -> {
                val duration = System.currentTimeMillis() - startTime
                val durationInSeconds = duration.toDouble() / 1000
                val blocksPerSecond = (blocksBroken.toDouble() / durationInSeconds).round(2)
                val cropName = event.crop.cropName
                LorenzUtils.chat("§e[SkyHanni] Stats for $cropName Contest:")
                val time = TimeUtils.formatDuration(duration - 999)
                LorenzUtils.chat("§e[SkyHanni] §7Blocks Broken in total: §e${blocksBroken.addSeparators()}")
                val color = getBlocksPerSecondColor(blocksPerSecond)
                LorenzUtils.chat("§e[SkyHanni] §7Average Blocks Per Second: $color$blocksPerSecond")
                LorenzUtils.chat("§e[SkyHanni] §7Participated for §b$time")
            }
            FarmingContestPhase.CHANGE -> {
                LorenzUtils.chat("§e[SkyHanni] You changed the crop during the contest, resetting the Blocks Per Second calculation..")
                startTime = System.currentTimeMillis()
            }
        }
        blocksBroken = 0
    }

    private fun getBlocksPerSecondColor(blocksPerSecond: Double) = if (blocksPerSecond > 19) "§c" else "§a"

    fun isEnabled() = GardenAPI.inGarden() && config.jacobContestSummary
}
