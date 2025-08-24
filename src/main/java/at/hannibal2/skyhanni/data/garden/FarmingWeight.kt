package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI.getCollection
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI.lastGainedCrop
import at.hannibal2.skyhanni.data.garden.CropCollectionAPI.updateTotalCollection
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteWeightsJson
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.api.ApiStaticGetPath
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.json.fromJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object FarmingWeight {
    val collectionMutex = Mutex()
    private val cropWeightValues = mutableMapOf<CropType, Double>()
    private val weightMap: MutableMap<EliteLeaderboardType, Double> = mutableMapOf()
    private var weightGain: Double = 0.0
    private var bonusWeight: Double = 0.0
    private var lastPlayerWeightFetch = SimpleTimeMark.farPast()
    private var attemptingCropWeightFetch = false
    private var hasFetchedCropWeights = false
    var apiError = false
    var profileId: String = ""
    private var shouldRecalculateWeight = false
    private var ignoredCollection = mutableMapOf<CropType, Long>()
    private var hasFetchedCollection = false

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (lastPlayerWeightFetch.passedSince() <= 5.minutes) return
        updateCollections()
    }

    @HandleEvent
    fun onCollectionUpdate(event: CropCollectionAddEvent) {
        if (event.cropCollectionType == CropCollectionType.MOOSHROOM_COW) {
            if (lastGainedCrop?.isAnyOf(CropType.CACTUS, CropType.SUGAR_CANE) == true) {
                addWeight(event.amount / (event.crop.getFactor() * 2))
                return
            }
        }
        addWeight(event.amount / event.crop.getFactor())
        if (weightGain >= 5.0) shouldRecalculateWeight = true // weight desyncs over time due to mushroom weight calc
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return

        SkyHanniMod.launchIOCoroutine {
            getCropWeights()
        }
    }

    fun setWeight(leaderboardType: EliteLeaderboardType, value: Double) {
        weightMap[leaderboardType] = value
        weightGain = 0.0
        FarmingWeightDisplay.update()
    }

    fun getWeight(leaderboardType: EliteLeaderboardType, override: Boolean = false): Double? {
        if (weightMap[leaderboardType] == null || override) {
            when (leaderboardType) {
                EliteLeaderboardType.ALL_TIME -> updateCollections()
                EliteLeaderboardType.MONTHLY -> getLeaderboardPosition(leaderboardType)
            }
        }
        if (shouldRecalculateWeight) {
            weightMap[EliteLeaderboardType.ALL_TIME] = recalculateTotalWeight()
        }
        return weightMap[leaderboardType]
    }

    private fun addWeight(amount: Double, type: EliteLeaderboardType? = null) {
        if (type == null) {
            weightMap.forEach{ (type, value) -> weightMap[type] = value + amount }
        } else {
            weightMap[type] = amount + (weightMap[type] ?: 0.0)
        }
        weightGain += amount
    }

    fun updateCollections() = SkyHanniMod.launchIOCoroutine {
        if (HypixelData.profileName == "") return@launchIOCoroutine
        if (collectionMutex.isLocked) return@launchIOCoroutine
        collectionMutex.withLock {
            val apiData = EliteDevApi.fetchWeightProfile(HypixelData.profileName) ?: run {
                if (weightMap.isEmpty()) {
                    apiError = true
                }
                return@launchIOCoroutine
            }
            profileId = apiData.profileId
            // we track this, so we only want elite values if they're higher or significantly different from what we have tracked
            apiData.crops.forEach { (name, value) ->
                run {
                    val crop = CropType.getByNameOrNull(name) ?: return@run
                    val storedAmount = crop.getCollection()
                    val diff = value - storedAmount
                    val weightDiff = abs(diff / crop.getFactor())
                    if (diff > 0 || weightDiff >= 10) { // 10 weight diff is at least half an hour of farming
                        crop.updateTotalCollection(value)
                    }
                }

            }
            // we don't track these
            apiData.uncountedCrops.forEach { (name, value) ->
                CropType.getByNameOrNull(name)?.let { ignoredCollection[it] = value.toLong() }
            }
            bonusWeight = apiData.bonusWeight.sumAllValues()

            weightGain = 0.0
            hasFetchedCollection = true
            shouldRecalculateWeight = true
            apiError = false
        }
    }

    private fun recalculateTotalWeight(): Double {
        val weightPerCrop = mutableMapOf<CropType, Double>()
        var totalWeight = 0.0
        for (crop in CropType.entries) {
            val weight = (crop.getCollection().minus(ignoredCollection[crop] ?: 0)) / crop.getFactor()
            weightPerCrop[crop] = weight
            totalWeight += weight
        }
        if (totalWeight > 0) {
            weightPerCrop[CropType.MUSHROOM] = specialMushroomWeight(weightPerCrop, totalWeight)
        }
        totalWeight = weightPerCrop.values.sum()
        weightGain = 0.0
        shouldRecalculateWeight = false
        return totalWeight + bonusWeight
    }

    private fun specialMushroomWeight(weightPerCrop: MutableMap<CropType, Double>, totalWeight: Double): Double {
        val cactusWeight = weightPerCrop[CropType.CACTUS]!!
        val sugarCaneWeight = weightPerCrop[CropType.SUGAR_CANE]!!
        val doubleBreakRatio = (cactusWeight + sugarCaneWeight) / totalWeight
        val normalRatio = (totalWeight - cactusWeight - sugarCaneWeight) / totalWeight

        val mushroomFactor = CropType.MUSHROOM.getFactor()
        val mushroomCollection = CropType.MUSHROOM.getCollection()
        return doubleBreakRatio * (mushroomCollection / (2 * mushroomFactor)) + normalRatio * (mushroomCollection / mushroomFactor)
    }

    fun reset() {
        cropWeightValues.clear()
        weightMap.clear()
        weightGain = 0.0
        bonusWeight = 0.0
        lastPlayerWeightFetch = SimpleTimeMark.farPast()
        attemptingCropWeightFetch = false
        hasFetchedCropWeights = false
        apiError = false
        profileId = ""
        shouldRecalculateWeight = false
        ignoredCollection = mutableMapOf()
        hasFetchedCollection = false
    }

    fun CropType.getFactor(): Double {
        val value = cropWeightValues[this] ?: backupCropWeights[this] ?: error("Crop $this not in backupFactors!")
        if (value != 0.0) return value else error("Crop $this weight factor is 0!")
    }

    // still needed when first joining garden and if they cant make https requests
    // TODO move to repo
    private val backupCropWeights = mapOf(
        CropType.WHEAT to 100_000.0,
        CropType.CARROT to 300_000.0,
        CropType.POTATO to 298_328.17,
        CropType.SUGAR_CANE to 198_885.45,
        CropType.NETHER_WART to 248_606.81,
        CropType.PUMPKIN to 99_236.12,
        CropType.MELON to 488_435.88,
        CropType.MUSHROOM to 90_944.27,
        CropType.COCOA_BEANS to 276_733.75,
        CropType.CACTUS to 178_730.65,
    )

    private val weightStatic = ApiStaticGetPath(
        "https://api.elitebot.dev/weights/all",
        "Elitebot Farming Weights",
    )

    private suspend fun getCropWeights() {
        if (attemptingCropWeightFetch || hasFetchedCropWeights) return
        attemptingCropWeightFetch = true
        val apiResponse = ApiUtils.getJsonResponse(weightStatic).assertSuccess() ?: return
        val apiResponseData = apiResponse.data ?: return
        val apiData = ConfigManager.gson.fromJson<EliteWeightsJson>(apiResponseData)
        for (crop in apiData.crops) {
            val cropType = CropType.getByNameOrNull(crop.key) ?: continue
            cropWeightValues[cropType] = crop.value
        }
        hasFetchedCropWeights = true
    }
}
