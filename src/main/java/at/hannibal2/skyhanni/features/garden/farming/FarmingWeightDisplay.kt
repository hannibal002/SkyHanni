package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.config.features.garden.EliteFarmingWeightConfig
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.jsonobjects.other.EliteLeaderboardJson
import at.hannibal2.skyhanni.data.jsonobjects.other.ElitePlayerWeightJson
import at.hannibal2.skyhanni.data.jsonobjects.other.EliteWeightsJson
import at.hannibal2.skyhanni.data.jsonobjects.other.UpcomingLeaderboardPlayer
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingWeightDisplay {

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { shouldShowDisplay() && isEnabled() },
            onRender = {
                config.pos.renderRenderables(display, posLabel = "Farming Weight Display")
            },
        )
    }

    private fun shouldShowDisplay(): Boolean =
        !GardenApi.hideExtraGuis() && (apiError || (config.ignoreLow || weight >= 200))

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        // Reset speed
        weightPerSecond = -1.0
    }

    @HandleEvent
    fun onWorldChange() {
        // We want to try to connect to the api again after a world switch.
        resetData()
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
        profileId = ""
        weight = -1.0
        apiWeight = 0.0
        shWeightDiff = 0.0

        nextPlayers.clear()
        rankGoal = -1
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        update()

        SkyHanniMod.coroutineScope.launch {
            getCropWeights()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(1, "garden.eliteFarmingWeightoffScreenDropMessage")
        event.move(3, "garden.eliteFarmingWeightDisplay", "garden.eliteFarmingWeights.display")
        event.move(3, "garden.eliteFarmingWeightPos", "garden.eliteFarmingWeights.pos")
        event.move(3, "garden.eliteFarmingWeightLeaderboard", "garden.eliteFarmingWeights.leaderboard")
        event.move(3, "garden.eliteFarmingWeightOvertakeETA", "garden.eliteFarmingWeights.overtakeETA")
        event.move(
            3,
            "garden.eliteFarmingWeightOffScreenDropMessage",
            "garden.eliteFarmingWeights.offScreenDropMessage",
        )
        event.move(3, "garden.eliteFarmingWeightOvertakeETAAlways", "garden.eliteFarmingWeights.overtakeETAAlways")
        event.move(3, "garden.eliteFarmingWeightETAGoalRank", "garden.eliteFarmingWeights.ETAGoalRank")
        event.move(3, "garden.eliteFarmingWeightIgnoreLow", "garden.eliteFarmingWeights.ignoreLow")
        event.move(14, "garden.eliteFarmingWeight.offScreenDropMessage", "garden.eliteFarmingWeights.showLbChange")
        event.move(34, "garden.eliteFarmingWeights.ETAGoalRank", "garden.eliteFarmingWeights.etaGoalRank")
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        if (!isEtaEnabled()) return
        if (lastupdate.passedSince() < 10.seconds) return

        ConditionalUtils.onToggle(config.eliteLBType) {
            // Reset api weight as different lb type will have a different score
            apiWeight = 0.0
            onConfigChanged()
        }

        ConditionalUtils.onToggle(config.useEtaGoalRank, config.etaGoalRank) {
            onConfigChanged()
        }
    }

    private fun onConfigChanged() {
        localCounter.clear()
        rankGoal = -1
        getRankGoal()
        loadLeaderboardIfAble()
        lastupdate = SimpleTimeMark.now()
    }

    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage get() = GardenApi.storage?.farmingWeight
    private val localCounter = mutableMapOf<CropType, Long>()

    private var display = emptyList<Renderable>()
    private var profileId = ""
    private var lastLeaderboardUpdate = SimpleTimeMark.farPast()
    private var apiError = false
    private var leaderboardPosition = -1
    private var weight = -1.0
    private var localWeight = 0.0
    private var weightPerSecond = -1.0
    private var weightNeedsRecalculating = false
    private var isLoadingWeight = false
    private var isLoadingLeaderboard = false
    private var rankGoal = -1
    private var minAmount = 0.0
    private var lastupdate: SimpleTimeMark = SimpleTimeMark.farPast()

    // Used to get the difference in weight to subtract for monthly lb
    // Caused by various inaccuracies, including pest calc
    private var shWeightDiff = 0.0
    private var apiWeight = 0.0
    // Calculated weight number to display
    private val displayWeight get() = localWeight + weight - shWeightDiff

    private val nextPlayers = mutableListOf<UpcomingLeaderboardPlayer>()
    private val nextPlayer get() = nextPlayers.firstOrNull()

    private val eliteWeightApiGson by lazy {
        BaseGsonBuilder.gson()
            .registerTypeAdapter(CropType::class.java, SkyHanniTypeAdapters.CROP_TYPE.nullSafe())
            .registerTypeAdapter(PestType::class.java, SkyHanniTypeAdapters.PEST_TYPE.nullSafe())
            .create()
    }

    private val errorMessage by lazy {
        listOf(
            "§cFarming Weight error: Cannot load",
            "§cdata from Elite Farmers!",
            "§eRejoin the garden or",
            "§eclick here to fix it.",
        ).map {
            Renderable.clickable(
                it,
                tips = listOf("§eClick here to reload the data right now!"),
                onLeftClick = ::resetData,
            )
        }
    }

    private var lastOpenWebsite = SimpleTimeMark.farPast()

    private fun update() {
        if (!isEnabled()) return
        if (apiError) {
            display = errorMessage
            return
        }

        if (weight == -1.0) {
            if (!isLoadingWeight) {
                val localProfile = HypixelData.profileName

                isLoadingWeight = true
                if (display.isEmpty()) {
                    display = listOf(RenderableString("§6${lbName()}§7: §eLoading.."))
                }
                SkyHanniMod.coroutineScope.launch {
                    loadWeight(localProfile)
                    isLoadingWeight = false
                }
            }
            return
        }

        val weight = getWeight()

        if (rankGoal == -1) rankGoal = getRankGoal()
        val leaderboard = getLeaderboard()

        val list = mutableListOf<Renderable>()
        list.add(
            Renderable.clickable(
                "§6${lbName()}§7: $weight$leaderboard",
                tips = listOf("§eClick to open your Farming Profile."),
                onLeftClick = { openWebsite(LorenzUtils.getPlayerName()) },
            ),
        )

        if (isEtaEnabled() && (weightPerSecond != -1.0 || config.overtakeETAAlways)) {
            getETA()?.let {
                list.add(it)
            }
        }
        display = list
    }

    private fun getLeaderboard(): String {
        if (!config.leaderboard) return ""

        // Fetching new leaderboard position every 10.5 minutes
        if (lastLeaderboardUpdate.passedSince() > 10.5.minutes) {
            loadLeaderboardIfAble()
        }

        return if (leaderboardPosition != -1) {
            val format = leaderboardPosition.addSeparators()
            " §7[§b#$format§7]"
        } else {
            if (isLoadingLeaderboard) " §7[§b#?§7]" else ""
        }
    }

    private fun getWeight(): String {
        if (weightNeedsRecalculating) {
            val values = calculateCollectionWeight().values
            if (values.isNotEmpty()) {
                localWeight = values.sum()
                weightNeedsRecalculating = false
            }
        }

        return "§e" + displayWeight.roundTo(2).addSeparators()
    }

    private fun getRankGoal(): Int {
        val value = config.etaGoalRank
        var goal = 10000

        // Check that the provided string is valid
        val parsed = value.get().toIntOrNull() ?: 0
        if (parsed < 1 || parsed > goal) {
            ChatUtils.chatAndOpenConfig(
                "Invalid Farming Weight Overtake Goal! Click here to edit the Overtake Goal config value " +
                    "to a valid number [1-10000] to use this feature!",
                config::etaGoalRank,
            )
            config.etaGoalRank.set(goal.toString())
        } else {
            goal = parsed
        }

        // Fetch the positions again if the goal was changed
        if (rankGoal != goal) {
            loadLeaderboardIfAble()
        }

        return goal
    }

    private fun getETA(): Renderable? {
        if (weight < 0) return null
        val nextPlayer = nextPlayer

        if (nextPlayer == null && weight > minAmount) {
            return Renderable.clickable(
                "§cWaiting for leaderboard update...",
                tips = listOf("§eClick here to load new data right now!"),
                onLeftClick = ::loadLeaderboardIfAble,
            )
        }
        val nextWeight = nextPlayer?.weight ?: minAmount
        var nextName = nextPlayer?.name ?: "$nextWeight Weight"

        val showRankGoal = (leaderboardPosition == -1 || leaderboardPosition > rankGoal) && config.useEtaGoalRank.get()
        nextName = if (showRankGoal) "#$rankGoal" else nextName

        var weightUntilOvertake = nextWeight - displayWeight

        if (weightUntilOvertake < 0) {
            if (weightPerSecond > 0) {
                farmingChatMessage("You passed §b$nextName §ein the §6${lbName()} §eLeaderboard!")
            }

            // Lower leaderboard position
            if (leaderboardPosition == -1) {
                leaderboardPosition = 10000
            } else {
                leaderboardPosition--
            }
            storage?.lastFarmingWeightLeaderboard = leaderboardPosition

            // Remove passed player to present the next one
            nextPlayers.removeFirst()

            // Display waiting message if nextPlayers list is empty
            // Update values to next player
            nextName = nextPlayer?.name ?: "Loading..."
            weightUntilOvertake = nextWeight - displayWeight
        }

        if (nextWeight == 0.0) {
            return Renderable.clickable(
                "§cRejoin the garden to show ETA!",
                tips = listOf("Click here to calculate the data right now!"),
                onLeftClick = ::loadLeaderboardIfAble,
            )
        }

        val timeFormat = if (weightPerSecond != -1.0) {
            val timeTillOvertake = try {
                (weightUntilOvertake / weightPerSecond).seconds
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e,
                    "Error calculating Farming ETA duration",
                    "weightPerSecond" to weightPerSecond,
                    "weightUntilOvertake" to weightUntilOvertake,
                    "totalWeight" to displayWeight,
                    "nextWeight" to nextWeight,
                )
                return null
            }
            val format = timeTillOvertake.format()
            " §7(§b$format§7)"
        } else ""

        // Adding 0.0 here to eliminate "-0"
        val weightFormat = (weightUntilOvertake.roundTo(2) + 0.0).addSeparators()
        val text = "§e$weightFormat$timeFormat §7behind §b$nextName"
        return if (showRankGoal) {
            Renderable.string(text)
        } else {
            Renderable.clickable(
                text,
                tips = listOf("§eClick to open the Farming Profile of §b$nextName."),
                onLeftClick = { openWebsite(nextName) },
            )
        }
    }

    private fun resetData() {
        apiError = false
        // We ask both api endpoints after every world switch
        weight = -1.0
        weightPerSecond = -1.0

        leaderboardPosition = -1
        weightNeedsRecalculating = true
        lastLeaderboardUpdate = SimpleTimeMark.farPast()

        nextPlayers.clear()
        rankGoal = -1

        localCounter.clear()
    }

    private fun farmingChatMessage(message: String) {
        ChatUtils.hoverableChat(
            message,
            listOf(
                "§eClick to open your Farming Weight",
                "§eprofile on §celitebot.dev",
            ),
            "/shfarmingprofile ${LorenzUtils.getPlayerName()}",
        )
    }

    private fun isEnabled() = config.display && (outsideEnabled() || inGardenEnabled())
    private fun outsideEnabled() = OutsideSBFeature.FARMING_WEIGHT.isSelected() && !LorenzUtils.inSkyBlock
    private fun inGardenEnabled() = (LorenzUtils.inSkyBlock && GardenApi.inGarden()) || config.showOutsideGarden

    private fun isEtaEnabled() = config.overtakeETA
    private fun isMonthlyLB() = config.eliteLBType.get() == EliteFarmingWeightConfig.EliteFarmingWeightLBType.MONTHLY

    fun addCrop(crop: CropType, addedCounter: Int) {
        // Prevent div-by-0 errors
        if (addedCounter == 0) return

        val before = getExactWeight()
        localCounter[crop] = crop.getLocalCounter() + addedCounter
        val after = getExactWeight()

        updateWeightPerSecond(crop, before, after, addedCounter)

        weightNeedsRecalculating = true
    }

    private fun updateWeightPerSecond(crop: CropType, before: Double, after: Double, diff: Int) {
        val speed = crop.getSpeed() ?: return
        val weightDiff = (after - before) * 1000
        weightPerSecond = (((weightDiff / diff) * speed) / 1000)
    }

    private fun getExactWeight(): Double {
        val values = calculateCollectionWeight().values
        return if (values.isNotEmpty()) {
            values.sum()
        } else 0.0
    }

    private fun loadLeaderboardIfAble() {
        if (isLoadingLeaderboard) return
        isLoadingLeaderboard = true

        SkyHanniMod.coroutineScope.launch {
            val wasNotLoaded = leaderboardPosition == -1
            leaderboardPosition = loadLeaderboardPosition()
            if (wasNotLoaded && config.showLbChange) {
                checkOffScreenLeaderboardChanges()
            }
            storage?.lastFarmingWeightLeaderboard = leaderboardPosition
            lastLeaderboardUpdate = SimpleTimeMark.now()
            isLoadingLeaderboard = false
        }
    }

    private fun checkOffScreenLeaderboardChanges() {
        val oldPosition = storage?.lastFarmingWeightLeaderboard ?: return

        if (oldPosition <= 0) return
        if (leaderboardPosition <= 0) return

        val diff = leaderboardPosition - oldPosition
        if (diff == 0) return

        if (diff > 0) {
            showLbChange("§cdropped ${StringUtils.pluralize(diff, "place", withNumber = true)}", oldPosition)
        } else {
            showLbChange("§arisen ${StringUtils.pluralize(-diff, "place", withNumber = true)}", oldPosition)
        }
    }

    private fun showLbChange(direction: String, oldPosition: Int) {
        farmingChatMessage(
            "§7Since your last visit to the §aGarden§7, " +
                "you have $direction §7on the §d${lbName()} Leaderboard§7. " +
                "§7(§e#${oldPosition.addSeparators()} §7-> §e#${leaderboardPosition.addSeparators()}§7)",
        )
    }

    private fun lbName() = "${if (isMonthlyLB()) "Monthly " else ""}Farming Weight"

    private fun loadLeaderboardPosition(): Int {
        val uuid = LorenzUtils.getPlayerUuid()

        // Fetch more upcoming players when the difference between ranks is expected to be tiny
        val upcomingPlayersParam = when {
            !isEnabled() -> ""
            leaderboardPosition > 10_000 -> "?upcoming=50"
            leaderboardPosition > 5_000 -> "?upcoming=30"
            leaderboardPosition > 1_000 -> "?upcoming=20"
            else -> "?upcoming=10"
        }
        // Tell the API to get upcoming players from our local rank (for when new data isn't fetched), or fallback to the
        // provided eta goal rank from the config
        val atRank = when {
            !isEtaEnabled() -> null
            config.useEtaGoalRank.get() && leaderboardPosition != -1 -> min(getRankGoal() + 1, leaderboardPosition)
            config.useEtaGoalRank.get() -> getRankGoal() + 1
            leaderboardPosition != -1 -> leaderboardPosition
            else -> null
        }
        val lbType = if (isMonthlyLB()) "-monthly" else ""
        val atRankParam = if (atRank != null) "&atRank=$atRank" else ""

        val url = "https://api.elitebot.dev/leaderboard/farmingweight$lbType/" +
            "$uuid/$profileId$upcomingPlayersParam$atRankParam"
        val apiResponse = ApiUtils.getJSONResponse(url, apiName = "Elitebot Farming Leaderboard")

        try {
            val apiData = toEliteLeaderboardJson(apiResponse).data
            val newData = apiWeight < apiData.amount

            minAmount = apiData.minAmount

            if (newData) {
                shWeightDiff = weight - apiData.amount
                apiWeight = apiData.amount
            }

            // Reset weight diff if not a monthly leaderboard
            if (apiData.initialAmount == 0.0) {
                shWeightDiff = 0.0
            }

            if (isEtaEnabled()) {
                nextPlayers.clear()
                apiData.upcomingPlayers.forEach {
                    if (it.weight > displayWeight) {
                        nextPlayers.add(it)
                    }
                }
            }

            // Keep local rank if new data wasn't returned
            return if (newData) apiData.rank else leaderboardPosition
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Error getting weight leaderboard position",
                "url" to url,
                "apiResponse" to apiResponse,
            )
        }
        return -1
    }

    private fun toEliteLeaderboardJson(obj: JsonObject): EliteLeaderboardJson {
        val jsonObject = JsonObject()
        jsonObject.add("data", obj)
        return eliteWeightApiGson.fromJson<EliteLeaderboardJson>(jsonObject)
    }

    private fun loadWeight(localProfile: String) {
        val uuid = LorenzUtils.getPlayerUuid()
        val url = "https://api.elitebot.dev/weight/$uuid"
        val apiResponse = ApiUtils.getJSONResponse(url, apiName = "Elite Farming Weight")

        var error: Throwable? = null

        try {
            val apiData = eliteWeightApiGson.fromJson<ElitePlayerWeightJson>(apiResponse)

            val selectedProfileId = apiData.selectedProfileId
            var selectedProfileEntry = apiData.profiles.find { it.profileId == selectedProfileId }

            if (selectedProfileEntry == null || (selectedProfileEntry.profileName.lowercase() != localProfile && localProfile != "")) {
                selectedProfileEntry = apiData.profiles.find { it.profileName.lowercase() == localProfile }
            }

            if (selectedProfileEntry != null) {
                profileId = selectedProfileEntry.profileId
                weight = selectedProfileEntry.totalWeight

                localCounter.clear()
                weightNeedsRecalculating = true
                return
            }

        } catch (e: Exception) {
            error = e
        }
        apiError = true

        ErrorManager.logErrorWithData(
            error ?: IllegalStateException("Error loading user farming weight"),
            "Error loading user farming weight\n" +
                "§eLoading the farming weight data from elitebot.dev failed!\n" +
                "§eYou can re-enter the garden to try to fix the problem.\n" +
                "§cIf this message repeats, please report it on Discord",
            "url" to url,
            "apiResponse" to apiResponse,
            "localProfile" to localProfile,
        )
    }

    private fun calculateCollectionWeight(): MutableMap<CropType, Double> {
        val weightPerCrop = mutableMapOf<CropType, Double>()
        var totalWeight = 0.0
        for (crop in CropType.entries) {
            val weight = crop.getLocalCounter() / crop.getFactor()
            weightPerCrop[crop] = weight
            totalWeight += weight
        }
        if (totalWeight > 0) {
            weightPerCrop[CropType.MUSHROOM] = specialMushroomWeight(weightPerCrop, totalWeight)
        }
        return weightPerCrop
    }

    private fun specialMushroomWeight(weightPerCrop: MutableMap<CropType, Double>, totalWeight: Double): Double {
        val cactusWeight = weightPerCrop[CropType.CACTUS]!!
        val sugarCaneWeight = weightPerCrop[CropType.SUGAR_CANE]!!
        val doubleBreakRatio = (cactusWeight + sugarCaneWeight) / totalWeight
        val normalRatio = (totalWeight - cactusWeight - sugarCaneWeight) / totalWeight

        val mushroomFactor = CropType.MUSHROOM.getFactor()
        val mushroomCollection = CropType.MUSHROOM.getLocalCounter()
        return doubleBreakRatio * (mushroomCollection / (2 * mushroomFactor)) + normalRatio * (mushroomCollection / mushroomFactor)
    }

    private fun CropType.getLocalCounter() = localCounter[this] ?: 0L

    private fun CropType.getFactor(): Double {
        return cropWeight[this] ?: backupCropWeights[this] ?: error("Crop $this not in backupFactors!")
    }

    private fun lookUpCommand(it: Array<String>) {
        val name = if (it.size == 1) it[0] else LorenzUtils.getPlayerName()
        openWebsite(name, ignoreCooldown = true)
    }

    private var lastName = ""

    private fun openWebsite(name: String, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && lastOpenWebsite.passedSince() < 5.seconds && name == lastName) return
        lastOpenWebsite = SimpleTimeMark.now()
        lastName = name

        OSUtils.openBrowser("https://elitebot.dev/@$name/")
        ChatUtils.chat("Opening Farming Profile of player §b$name")
    }

    private val cropWeight = mutableMapOf<CropType, Double>()
    private var attemptingCropWeightFetch = false
    private var hasFetchedCropWeights = false

    private fun getCropWeights() {
        if (attemptingCropWeightFetch || hasFetchedCropWeights) return
        attemptingCropWeightFetch = true
        val url = "https://api.elitebot.dev/weights/all"
        val apiResponse = ApiUtils.getJSONResponse(url, apiName = "Elitebot Farming Weight")

        try {
            val apiData = eliteWeightApiGson.fromJson<EliteWeightsJson>(apiResponse)
            apiData.crops
            for (crop in apiData.crops) {
                val cropType = CropType.getByNameOrNull(crop.key) ?: continue
                cropWeight[cropType] = crop.value
            }
            hasFetchedCropWeights = true
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Error getting crop weights from elitebot.dev",
                "apiResponse" to apiResponse,
            )
        }
    }

    // still needed when first joining garden and if they cant make https requests
    private val backupCropWeights by lazy {
        mapOf(
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
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shfarmingprofile") {
            description = "Look up the farming profile from yourself or another player on elitebot.dev"
            category = CommandCategory.USERS_ACTIVE
            callback { lookUpCommand(it) }
        }
    }
}
