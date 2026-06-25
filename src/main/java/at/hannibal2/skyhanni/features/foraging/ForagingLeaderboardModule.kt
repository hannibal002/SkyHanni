package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardRankConfig
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.foraging.ForagingCollectionApi.lastGainedCollectionTime
import at.hannibal2.skyhanni.data.foraging.ForagingCollectionApi.lastGainedLog
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearCategories
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearEntries
import at.hannibal2.skyhanni.data.garden.FarmingWeightData
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.foraging.ForagingCollectionAddEvent
import at.hannibal2.skyhanni.features.foraging.ForagingLogType.Companion.getForagingLogType
import at.hannibal2.skyhanni.features.garden.leaderboarddisplays.ForagingDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ForagingLeaderboardModule {
    private val config get() = SkyHanniMod.feature.foraging.leaderboard
    private val debugConfig get() = config.debug
    private val display = ForagingDisplay()
    private var lastDebugLog = SimpleTimeMark.farPast()
    private var lastForceDisplay = false

    // Mark the display stale on collection gain so update() runs on the render thread.
    @HandleEvent
    fun onForagingCollectionAdd(event: ForagingCollectionAddEvent) {
        if (!config.enabled) return
        display.lastUpdate = SimpleTimeMark.farPast()
    }

    // Mark the display stale on log click so the type switch happens on the next render frame.
    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockClick(event: BlockClickEvent) {
        if (!config.enabled) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (!IslandTypeTag.FORAGING.isInIsland() && !IslandType.HUB.isInIsland()) return
        event.blockState.getForagingLogType() ?: return
        display.lastUpdate = SimpleTimeMark.farPast()
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        val inSkyBlock = SkyBlockUtils.inSkyBlock
        val inForagingIsland = IslandTypeTag.FORAGING.isInIsland() || IslandType.HUB.isInIsland() || IslandType.PRIVATE_ISLAND.isInIsland()
        val forceDisplay = debugConfig.forceDisplay

        if (forceDisplay != lastForceDisplay) {
            lastForceDisplay = forceDisplay
            clearCategories(EliteLeaderboardType.ForagingLog::class)
            display.reset()
        }

        if (!inSkyBlock || !config.enabled || (!forceDisplay && !inForagingIsland)) {
            logDebug(
                "skip render inSkyBlock=$inSkyBlock enabled=${config.enabled} inForagingIsland=$inForagingIsland " +
                    "forceDisplay=$forceDisplay lastGainedLog=$lastGainedLog",
            )
            return
        }


        logDebug(
            "render foraging lb forceDisplay=$forceDisplay forcedType=${debugConfig.forcedLogType.get()} " +
                "lastGainedLog=$lastGainedLog profileIdBlank=${FarmingWeightData.profileId.isBlank()} " +
                "lastGainAge=${lastGainedCollectionTime.passedSince()}",
        )
        display.renderDisplay(config.position)
    }

    private fun logDebug(message: String) {
        if (!debugConfig.verboseLogging) return
        if (lastDebugLog.passedSince() < 3.seconds) return
        lastDebugLog = SimpleTimeMark.now()
        ChatUtils.debug("[Foraging LB Debug] $message")
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val foragingConfigs = listOf(
            config.rankGoals.useRankGoal,
            config.rankGoals.rankGoalTypes,
            config.gamemode,
            config.debug.forcedLogType,
        )
        foragingConfigs.forEach {
            it.afterChange {
                clearCategories(EliteLeaderboardType.ForagingLog::class)
                display.reset()
            }
        }
        for (log in ForagingLogType.entries) {
            for (mode in EliteLeaderboardMode.entries) {
                val leaderboardType = EliteLeaderboardType.ForagingLog(log, mode)
                ConditionalUtils.onToggle(getLeaderboardRankConfig(leaderboardType)?.get() ?: continue) {
                    clearEntries(leaderboardType)
                    display.reset()
                }
            }
        }
    }
}
