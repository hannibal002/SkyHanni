package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi.lastPestKillTimes
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

class PestDisplay : EliteLeaderboardDisplayBase<PestType, EliteLeaderboardType.Pest>(
    EliteLeaderboardType.Pest::class,
    { pest, mode -> EliteLeaderboardType.Pest(pest, mode) },
    name = "Pest Leaderboard Display"
) {
    val config get() = configBase.pestKillsLeaderboard
    private val pestStorage get() = GardenApi.storage?.farmingWeight?.pestDisplayType

    override var currentMode: EliteLeaderboardMode
        get() = pestStorage?.mode ?: EliteLeaderboardMode.ALL_TIME
        set(value) { pestStorage?.mode = value }

    override var currentEnum: PestType?
        get() = pestStorage?.enum
        set(value) { pestStorage?.enum = value }

    override fun getDefaultEnum(): PestType? {
        return null
    }

    override val currentLeaderboardType: EliteLeaderboardType
        get() = EliteLeaderboardType.Pest(currentEnum, currentMode)

    // We don't track pest kills over a time period so we can't support this right now
    override fun overtakeEta(amountUntil: Double): String {
        return ""
    }

    override fun MutableList<Renderable>.buildTypeSwitcher() {
        this.addRenderableNullableButton(
            label = "Pest Type",
            current = currentEnum,
            nullLabel = "All",
            onChange = { new ->
                currentEnum = new
                update()
            },
            universe = PestType.filterableEntries,
        )
    }

    // basing this on pest profit tracker
    override fun shouldShowDisplay(): Boolean {
        if (GardenApi.hideExtraGuis()) return false
        if (!config.display.hideWhenInactive) return true
        val allInactive = lastPestKillTimes.all {
            it.value.passedSince() > config.display.timeDisplayed.seconds
        }
        val notHoldingTool = !PestApi.hasVacuumInHand() && !PestApi.hasSprayonatorInHand()
        return !(allInactive && notHoldingTool)
    }
}

data class PestLeaderboardStorage(
    @Expose var enum: PestType?,
    @Expose var mode: EliteLeaderboardMode
)
