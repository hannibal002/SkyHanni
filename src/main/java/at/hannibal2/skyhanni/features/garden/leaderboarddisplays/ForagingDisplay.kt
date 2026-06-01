package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.foraging.ForagingCollectionApi
import at.hannibal2.skyhanni.data.foraging.ForagingCollectionApi.lastGainedCollectionTime
import at.hannibal2.skyhanni.data.foraging.ForagingCollectionApi.lastGainedLog
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.foraging.ForagingLogType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.minutes

class ForagingDisplay : EliteLeaderboardDisplayBase<ForagingLogType, EliteLeaderboardType.ForagingLog>(
    EliteLeaderboardType.ForagingLog::class,
    { log, mode -> EliteLeaderboardType.ForagingLog(log, mode) },
    name = "Foraging Leaderboard Display"
) {
    private val foragingConfig get() = SkyHanniMod.feature.foraging.leaderboard
    private val debugConfig get() = foragingConfig.debug
    private val foragingStorage get() = GardenApi.storage?.farmingWeight?.foragingLogDisplayType
    override var currentMode: EliteLeaderboardMode
        get() = foragingStorage?.mode ?: EliteLeaderboardMode.ALL_TIME
        set(value) { foragingStorage?.mode = value }
    override var currentEnum: ForagingLogType?
        get() = foragingStorage?.enum
        set(value) { foragingStorage?.enum = value }

    // session fallback in case profile storage isn't available yet
    private var sessionLastLog: ForagingLogType? = null

    override fun getDefaultEnum(): ForagingLogType? {
        // prefer the actively chopped type, then last collected, then session cache
        val log = when {
            debugConfig.forceDisplay -> debugConfig.forcedLogType.get()
            else -> ForagingCollectionApi.getCurrentlyChopping() ?: lastGainedLog
        }
        if (log != null) sessionLastLog = log
        return log ?: sessionLastLog
    }

    override fun isEnabled(): Boolean =
        foragingConfig.enabled && SkyBlockUtils.inSkyBlock &&
            (debugConfig.forceDisplay || IslandTypeTag.FORAGING.isInIsland() || IslandType.HUB.isInIsland() || IslandType.PRIVATE_ISLAND.isInIsland())

    override fun inIslandEnabled(): Boolean =
        debugConfig.forceDisplay || IslandTypeTag.FORAGING.isInIsland() || IslandType.HUB.isInIsland() || IslandType.PRIVATE_ISLAND.isInIsland()
    // No live foraging speed tracking, so no ETA
    override fun overtakeEta(amountUntil: Double): String = ""
    override fun MutableList<Renderable>.buildTypeSwitcher() {
        this.addRenderableNullableButton(
            label = "Log Type",
            current = currentEnum,
            nullLabel = "Default",
            onChange = { new ->
                currentEnum = new
                update()
            },
            universe = ForagingLogType.entries,
        )
    }
    override fun shouldShowDisplay(): Boolean {
        val inAllowedIsland = IslandTypeTag.FORAGING.isInIsland() || IslandType.HUB.isInIsland() || IslandType.PRIVATE_ISLAND.isInIsland()
        if (!debugConfig.forceDisplay && !inAllowedIsland) return false
        if (debugConfig.forceDisplay) return true
        if (currentEnum != null) return true
        // lastGainedLog is persisted across sessions; sessionLastLog covers transient storage gaps
        if (lastGainedLog != null || sessionLastLog != null) return true
        return lastGainedCollectionTime.passedSince() < 2.minutes
    }
}

data class ForagingLeaderboardStorage(
    @Expose var enum: ForagingLogType?,
    @Expose var mode: EliteLeaderboardMode
)
