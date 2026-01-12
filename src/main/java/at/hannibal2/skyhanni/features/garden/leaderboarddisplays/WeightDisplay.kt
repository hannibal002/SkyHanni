package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getFactor
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.FarmingWeight
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getLatestBlocksPerSecond
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

class WeightDisplay : EliteLeaderboardDisplayBase<FarmingWeight, EliteLeaderboardType.Weight>(
    EliteLeaderboardType.Weight::class,
    { weight, mode -> EliteLeaderboardType.Weight(weight, mode) },
    name = "Farming Weight Display"
) {
    val config get() = configBase.farmingWeightLeaderboard
    private var lastFarmedCrop: CropType? = null

    private val weightStorage get() = GardenApi.storage?.farmingWeight?.weightDisplayType

    override var currentMode: EliteLeaderboardMode
        get() = weightStorage?.mode ?: EliteLeaderboardMode.ALL_TIME
        set(value) { weightStorage?.mode = value }

    override var currentEnum: FarmingWeight?
        get() = weightStorage?.enum ?: FarmingWeight.FARMING_WEIGHT
        set(value) { weightStorage?.enum = value }

    override fun getDefaultEnum(): FarmingWeight {
        return FarmingWeight.FARMING_WEIGHT
    }

    override fun overtakeEta(amountUntil: Double): String {
        if (!config.display.overtakeETA.get() || !config.display.overtakeETAAlways.get() && !GardenApi.isCurrentlyFarming()) return ""
        lastFarmedCrop = GardenApi.getCurrentlyFarmedCrop() ?: if (config.display.overtakeETAAlways.get()) lastFarmedCrop else null
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

    override fun MutableList<Renderable>.buildTypeSwitcher() = Unit

    override fun shouldShowDisplay(): Boolean =
        !GardenApi.hideExtraGuis() && (apiError || (config.display.ignoreLow || (getWeight(EliteLeaderboardMode.ALL_TIME) ?: 0.0) >= 200.0))
}

data class WeightLeaderboardStorage(
    @Expose var enum: FarmingWeight?,
    @Expose var mode: EliteLeaderboardMode
)
