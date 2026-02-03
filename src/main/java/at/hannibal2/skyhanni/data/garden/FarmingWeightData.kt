package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.getCollection
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.lastGainedCrop
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.setCollectionCounter
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteWeightsJson
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.FarmingWeight
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.api.ApiStaticGetPath
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.json.fromJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingWeightData {
    var apiError = false
    var profileId: String = ""

    private val collectionMutex = Mutex()
    private val cropWeightValues = mutableMapOf<CropType, Double>()
    private val weightMap: MutableMap<EliteLeaderboardMode, Double> = mutableMapOf()
    private val ignoredCollection: MutableMap<CropType, Long> = mutableMapOf()

    private var weightGain: Double = 0.0
    private var bonusWeight: Double = 0.0
    private var lastPlayerWeightFetch = SimpleTimeMark.farPast()
    private var lastFetchAttempt = SimpleTimeMark.farPast()
    private var fetchAttempts = 0
    private var attemptingCropWeightFetch = false
    private var hasFetchedCropWeights = false
    private var shouldRecalculateWeight = false

    @HandleEvent
    fun onWorldChange(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.GARDEN) return
        updateCollections()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onProfileJoin(event: ProfileJoinEvent) {
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

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return

        SkyHanniMod.launchIOCoroutine("get crop weights") {
            getCropWeights()
        }
    }

    fun setWeight(leaderboardMode: EliteLeaderboardMode, value: Double) {
        weightMap[leaderboardMode] = value
        weightGain = 0.0
    }

    fun getWeight(leaderboardMode: EliteLeaderboardMode, override: Boolean = false, cropWeightOnly: Boolean = false): Double? {
        if (weightMap[leaderboardMode] == null || override) {
            when (leaderboardMode) {
                EliteLeaderboardMode.ALL_TIME -> {
                    // we only update collections on garden join
                }
                EliteLeaderboardMode.MONTHLY ->
                    getLeaderboardPosition(EliteLeaderboardType.Weight(FarmingWeight.FARMING_WEIGHT, leaderboardMode))
            }
        }
        if (shouldRecalculateWeight) {
            weightMap[EliteLeaderboardMode.ALL_TIME] = recalculateTotalWeight()
        }
        val weight = weightMap[leaderboardMode]
        if (cropWeightOnly) {
            if (weight != null) {
                return weight - bonusWeight
            }
        }
        return weight
    }

    private fun addWeight(amount: Double, type: EliteLeaderboardMode? = null) {
        if (type == null) {
            weightMap.forEach { (type, value) -> weightMap[type] = value + amount }
        } else {
            weightMap[type] = amount + (weightMap[type] ?: 0.0)
        }
        weightGain += amount
    }

    fun updateCollections(ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && (lastFetchAttempt.passedSince() <= 30.seconds || lastPlayerWeightFetch.passedSince() <= 15.minutes)) return
        if (HypixelData.profileName.isEmpty()) return
        if (collectionMutex.isLocked) return
        lastFetchAttempt = SimpleTimeMark.now()
        fetchAttempts++
        if (fetchAttempts >= 3) {
            lastPlayerWeightFetch = SimpleTimeMark.now()
            fetchAttempts = 0
        }
        fetchCollections()
    }

    private fun fetchCollections() = SkyHanniMod.launchIOCoroutine("fetch collections", timeout = 30.seconds) {
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
                    // elite only updates data every 2 hours or so
                    if (diff > 0 || weightDiff >= 100) { // || apiData.lastUpdated > CropCollectionApi.lastGainedCollectionTime
                        crop.setCollectionCounter(value)
                    }
                }

            }
            // we don't track these so always prefer api data
            apiData.uncountedCrops.forEach { (name, value) ->
                CropType.getByNameOrNull(name)?.let { ignoredCollection[it] = value.toLong() }
            }
            bonusWeight = apiData.bonusWeight.sumAllValues()

            weightGain = 0.0
            shouldRecalculateWeight = true
            lastPlayerWeightFetch = SimpleTimeMark.now()
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
        val cactusWeight = weightPerCrop[CropType.CACTUS] ?: -1.0
        val sugarCaneWeight = weightPerCrop[CropType.SUGAR_CANE] ?: -1.0
        val doubleBreakRatio = (cactusWeight + sugarCaneWeight) / totalWeight
        val normalRatio = (totalWeight - cactusWeight - sugarCaneWeight) / totalWeight

        val mushroomFactor = CropType.MUSHROOM.getFactor()
        val mushroomCollection = CropType.MUSHROOM.getCollection()
        return doubleBreakRatio * (mushroomCollection / (2 * mushroomFactor)) + normalRatio * (mushroomCollection / mushroomFactor)
    }

    fun reset() {
        cropWeightValues.clear()
        weightMap.clear()
        ignoredCollection.clear()
        weightGain = 0.0
        bonusWeight = 0.0
        lastPlayerWeightFetch = SimpleTimeMark.farPast()
        lastFetchAttempt = SimpleTimeMark.farPast()
        fetchAttempts = 0
        attemptingCropWeightFetch = false
        hasFetchedCropWeights = false
        apiError = false
        profileId = ""
        shouldRecalculateWeight = false
    }

    fun CropType.getFactor(): Double {
        val value = cropWeightValues[this] ?: backupCropWeights[this] ?: error("Crop $this not in backupFactors!")
        if (value != 0.0) return value else error("Crop $this weight factor is 0!")
    }

    // still needed when first joining garden and if they cant make https requests
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
        CropType.MOONFLOWER to 200_000.0,
        CropType.SUNFLOWER to 200_000.0,
        CropType.WILD_ROSE to 200_000.0,
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

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shfarmingprofile") {
            description = "Look up the farming profile from yourself or another player on elitebot.dev"
            category = CommandCategory.USERS_ACTIVE
            argCallback("name", BrigadierArguments.string()) { name ->
                openWebsite(name, ignoreCooldown = true)
            }
            simpleCallback {
                openWebsite(PlayerUtils.getName(), ignoreCooldown = true)
            }
        }
    }

    private var lastName = ""
    private var lastOpenWebsite = SimpleTimeMark.farPast()

    fun openWebsite(name: String, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && lastOpenWebsite.passedSince() < 5.seconds && name == lastName) return
        lastOpenWebsite = SimpleTimeMark.now()
        lastName = name

        OSUtils.openBrowser("https://elitebot.dev/@$name/")
        ChatUtils.chat("Opening Farming Profile of player §b$name")
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("farming weight")
        event.addIrrelevant {
            CropType.entries.forEach {
                add("$it - Weight Factor: ${cropWeightValues[it]}")
            }
        }
    }
}
