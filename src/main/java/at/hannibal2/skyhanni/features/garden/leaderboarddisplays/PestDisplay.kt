package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import com.google.gson.annotations.Expose

class PestDisplay : EliteLeaderboardDisplayBase<PestType, EliteLeaderboardType.Pest>(
    EliteLeaderboardType.Pest::class,
    { pest, mode -> EliteLeaderboardType.Pest(pest, mode) },
    name = "Pest Leaderboard Display"
) {
    val config get() = configBase.pestKillsDisplay
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

    override val currentLeaderboardType: EliteLeaderboardType?
        get() = EliteLeaderboardType.Pest(currentEnum, currentMode)

    // We don't track pest kills over a time period so we can't support this right now
    override fun overtakeEta(amountUntil: Double): String {
        return ""
    }

    private fun changeEnum(pestType: PestType?) {
        if (pestType != null) currentMode = EliteLeaderboardMode.ALL_TIME // Specific pest lbs don't support monthly
        currentEnum = pestType
        update()
    }

    override fun MutableList<Renderable>.buildTypeSwitcher() {
        this.addRenderableNullableButton(
            label = "Pest Type",
            current = currentEnum,
            nullLabel = "All",
            onChange = { new ->
                changeEnum(new)
            },
            universe = PestType.filterableEntries,
            enableUniverseScroll = false // would infinitely scroll while hovered
        )
    }

    override fun shouldShowDisplay(): Boolean = !GardenApi.hideExtraGuis()
}

data class PestLeaderboardStorage(
    @Expose var enum: PestType?,
    @Expose var mode: EliteLeaderboardMode
)
