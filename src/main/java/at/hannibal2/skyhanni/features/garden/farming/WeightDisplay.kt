package at.hannibal2.skyhanni.features.garden.farming

import EliteLeaderboardDisplay
import at.hannibal2.skyhanni.config.features.garden.leaderboards.FarmingWeightDisplayConfig.FarmingWeightTextEntry
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.FarmingWeightData
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.FarmingWeight
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton

class WeightDisplay: EliteLeaderboardDisplay<FarmingWeight, EliteLeaderboardType.Weight>(
    GardenApi.storage?.farmingWeight?.weightDisplayType,
    { weight, mode -> EliteLeaderboardType.Weight(weight, mode) },
    name = "Farming Weight Display"
) {
    val config get() = configBase.farmingWeightDisplay

    override fun getDefaultEnum(): FarmingWeight {
        return FarmingWeight.FARMING_WEIGHT // TODO set actual default
    }

    override fun drawDisplay(leaderboardType: EliteLeaderboardType) {
        if (!isEnabled()) return

        val isFirst = leaderboardPos == 1
        val lineMap = mutableMapOf<FarmingWeightTextEntry, Renderable>()

        lineMap[FarmingWeightTextEntry.WEIGHT_POSITION] = weightPosRenderable(leaderboardType)
        lineMap[FarmingWeightTextEntry.OVERTAKE] = overtakeRenderable(leaderboardType, isFirst)
        if (!isFirst && config.text.get().contains(FarmingWeightTextEntry.OVERTAKE)) {
            lineMap[FarmingWeightTextEntry.LAST_PLAYER] = overtakeRenderable(leaderboardType, true)
        }

        display = formatDisplay(lineMap)
    }

    override fun overtakeEta(weightUntil: Double): String {
        return ""
    }

    override fun useEtaGoalRank(): Boolean {
        return config.useEtaGoalRank.get()
    }

    override fun showLeaderboard(): Boolean = config.leaderboard.get()

    // TODO consider abstracting this to remove duplication
    private fun formatDisplay(lineMap: MutableMap<FarmingWeightTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeightData.apiError || EliteFarmersLeaderboard.apiError) {
            return errorMessage
        }

        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildModeSwitcher() else newList.addVerticalSpacer()
        newList.addAll(config.text.get().mapNotNull { lineMap[it] })
        return newList
    }

    override fun MutableList<Renderable>.buildTypeSwitcher() {} // No switcher needed for this display

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun isEnabled(): Boolean = config.display && (inGardenEnabled())

    private fun inGardenEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)

    override fun shouldShowDisplay(): Boolean {
        return true
    }


}
