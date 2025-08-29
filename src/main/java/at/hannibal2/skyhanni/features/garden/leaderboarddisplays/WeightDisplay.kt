package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.features.garden.leaderboards.FarmingWeightDisplayConfig.FarmingWeightTextEntry
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.isUnranked
import at.hannibal2.skyhanni.data.garden.FarmingWeightData
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getFactor
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.FarmingWeight
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getLatestBlocksPerSecond
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import kotlin.time.Duration.Companion.seconds

class WeightDisplay: EliteLeaderboardDisplayBase<FarmingWeight, EliteLeaderboardType.Weight>(
    GardenApi.storage?.farmingWeight?.weightDisplayType,
    { weight, mode -> EliteLeaderboardType.Weight(weight, mode) },
    name = "Farming Weight Display"
) {
    val config get() = configBase.farmingWeightDisplay
    var lastFarmedCrop: CropType? = null

    override fun getDefaultEnum(): FarmingWeight {
        return FarmingWeight.FARMING_WEIGHT // TODO set actual default
    }

    override fun drawDisplay(leaderboardType: EliteLeaderboardType) {
        if (!isEnabled()) return

        val isFirst = leaderboardPos == 1
        val lineMap = mutableMapOf<FarmingWeightTextEntry, Renderable>()

        lineMap[FarmingWeightTextEntry.WEIGHT_POSITION] = weightPosRenderable(leaderboardType)
        lineMap[FarmingWeightTextEntry.OVERTAKE] = overtakeRenderable(leaderboardType, isFirst)
        if (!isFirst && !isUnranked(leaderboardType) && config.text.get().contains(FarmingWeightTextEntry.OVERTAKE)) {
            lineMap[FarmingWeightTextEntry.LAST_PLAYER] = overtakeRenderable(leaderboardType, true)
        }

        display = formatDisplay(lineMap)
    }

    override fun overtakeEta(amountUntil: Double): String {
        if (!config.overtakeETA.get() || !config.overtakeETAAlways.get() && !GardenApi.isCurrentlyFarming()) return ""
        lastFarmedCrop = GardenApi.getCurrentlyFarmedCrop() ?: if (config.overtakeETAAlways.get()) lastFarmedCrop else null
        val crop = lastFarmedCrop ?: return ""
        val cropsPerSecond = crop.getSpeed() ?: return ""
        val mooshroomCowCropsPerSecond = if (GardenApi.mushroomCowPet) {
            (CurrentPetApi.currentPet?.level ?: 0) / 100 * (crop.getLatestBlocksPerSecond() ?: 0.0)
        } else {
            0.0
        }
        val weightPerSecond = cropsPerSecond / crop.getFactor() + mooshroomCowCropsPerSecond / CropType.MUSHROOM.getFactor()
        val timeUntil = (amountUntil / weightPerSecond).seconds
        return " §7(§b${timeUntil.format()}§7)"
    }

    override fun useEtaGoalRank(): Boolean {
        return config.useRankGoal.get()
    }

    override fun showLeaderboard(): Boolean = config.leaderboard.get()

    private fun formatDisplay(lineMap: MutableMap<FarmingWeightTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeightData.apiError || EliteFarmersLeaderboard.apiError) return errorMessage

        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildModeSwitcher() else newList.addVerticalSpacer()
        newList.addAll(config.text.get().mapNotNull { lineMap[it] })
        return newList
    }

    override fun MutableList<Renderable>.buildTypeSwitcher() {} // No switcher needed for this display

    override fun isEnabled(): Boolean = config.display && (inGardenEnabled())

    private fun inGardenEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)

    override fun shouldShowDisplay(): Boolean =
        !GardenApi.hideExtraGuis() && (apiError || (config.ignoreLow || (getWeight(EliteLeaderboardMode.ALL_TIME) ?: 0.0) >= 200.0))


}
