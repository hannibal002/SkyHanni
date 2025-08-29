package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.config.features.garden.leaderboards.PestKillsDisplayConfig.PestKillsTextEntry
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.isUnranked
import at.hannibal2.skyhanni.data.garden.FarmingWeightData
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton

class PestDisplay : EliteLeaderboardDisplayBase<PestType, EliteLeaderboardType.Pest>(
    GardenApi.storage?.farmingWeight?.pestDisplayType,
    { pest, mode -> EliteLeaderboardType.Pest(pest, mode) },
    name = "Pest Leaderboard Display"
) {
    val config get() = configBase.pestKillsDisplay

    override fun getDefaultEnum(): PestType? {
        return null
    }

    override val currentLeaderboardType: EliteLeaderboardType?
        get() = EliteLeaderboardType.Pest(currentEnum, currentMode)

    override fun drawDisplay(leaderboardType: EliteLeaderboardType) {
        if (!isEnabled()) return

        val lineMap = mutableMapOf<PestKillsTextEntry, Renderable>()
        val isFirst = leaderboardPos == 1

        lineMap[PestKillsTextEntry.WEIGHT_POSITION] = weightPosRenderable(leaderboardType)
        lineMap[PestKillsTextEntry.OVERTAKE] = overtakeRenderable(leaderboardType, isFirst)
        if (!isFirst && !isUnranked(leaderboardType) && config.text.get().contains(PestKillsTextEntry.OVERTAKE)) {
            lineMap[PestKillsTextEntry.LAST_PLAYER] = overtakeRenderable(leaderboardType, true)
        }

        display = formatDisplay(lineMap)
    }

    // We don't track pest kills over a time period so we can't support this right now
    override fun overtakeEta(amountUntil: Double): String {
        return ""
    }

    override fun useEtaGoalRank(): Boolean {
        return config.useRankGoal.get()
    }

    override fun showLeaderboard(): Boolean = config.leaderboard.get()

    private fun formatDisplay(lineMap: MutableMap<PestKillsTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeightData.apiError || EliteFarmersLeaderboard.apiError) {
            return errorMessage
        }

        val newList = mutableListOf<Renderable>()
        if (inventoryOpen && currentEnum == null) newList.buildModeSwitcher() else newList.addVerticalSpacer()
        newList.addAll(config.text.get().mapNotNull { lineMap[it] })
        if (inventoryOpen) newList.buildTypeSwitcher() else newList.addVerticalSpacer()
        return newList
    }

    private fun changeEnum(pestType: PestType?) {
        if (pestType != null) currentMode = EliteLeaderboardMode.ALL_TIME // Specific pest lbs don't support monthly
        currentEnum = pestType
        update()
    }

    private fun MutableList<Renderable>.buildTypeSwitcher() {
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

    override fun isEnabled(): Boolean = config.display && (inGardenEnabled())

    private fun inGardenEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)

    override fun shouldShowDisplay(): Boolean = !GardenApi.hideExtraGuis()


}
