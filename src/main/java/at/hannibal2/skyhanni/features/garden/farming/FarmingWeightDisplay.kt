package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.EliteFarmingWeightConfig
import at.hannibal2.skyhanni.config.features.garden.EliteFarmingWeightConfig.FarmingWeightTextEntry
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.CropMilestonesConfig.MilestoneTextEntry
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getRankGoal
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.loadingLeaderboardMutex
import at.hannibal2.skyhanni.data.garden.FarmingWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getWeight
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesAPI.inaccurateMilestone
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.UpcomingLeaderboardPlayer
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import scala.concurrent.duration.Duration.Infinite
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingWeightDisplay {

    /*init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { shouldShowDisplay() && isEnabled() },
            onRender = {
                config.pos.renderRenderables(display, posLabel = "Farming Weight Display")
            },
        )
    }*/

    private fun shouldShowDisplay(): Boolean =
        !GardenApi.hideExtraGuis() && (apiError || (config.ignoreLow || weight >= 200))

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled() || !shouldShowDisplay()) return
        val currentlyOpen = InventoryUtils.inAnyInventory()

        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        config.pos.renderRenderables(display, posLabel = "Farming Weight Display")
    }

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        // Reset speed
        weightPerSecond = -1.0
    }

    /*@HandleEvent
    fun onWorldChange() {
        // We want to try to connect to the api again after a world switch.
        resetData()
    }*/

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
        profileId = ""
        weight = -1.0
        apiWeight = 0.0
        shWeightDiff = 0.0

        //nextPlayers.clear()
        rankGoal = -1
    }

    /*@HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return

        SkyHanniMod.launchIOCoroutine {
            update()
            //getCropWeights()
        }
    }*/

    @HandleEvent
    fun onCollectionUpdate(event: CollectionUpdateEvent) {
        if (!isEnabled()) return
        update()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        //check if eta is enabled
        update()
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        if (!isEtaEnabled()) return
        if (lastUpdate.passedSince() < 10.seconds) return

        /*ConditionalUtils.onToggle(config.eliteLBType) {
            // Reset api weight as different lb type will have a different score
            apiWeight = 0.0
            onConfigChanged()
        }*/

        ConditionalUtils.onToggle(config.useEtaGoalRank, config.etaGoalRank) {
            onConfigChanged()
        }
    }

    private fun onConfigChanged() {
        localCounter.clear()
        rankGoal = -1
        //getRankGoal()
        //loadLeaderboardIfAble()
        lastUpdate = SimpleTimeMark.now()
    }

    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage get() = GardenApi.storage?.farmingWeight
    private val lbName get() = currentLeaderboardType.specificDisplayName
    private val localCounter = mutableMapOf<CropType, Long>()
    private var display = emptyList<Renderable>()
    private var profileId = ""
    private var lastLeaderboardUpdate = SimpleTimeMark.farPast()
    private var apiError = false
    //private var leaderboardPosition = -1
    private var weight = -1.0
    private var localWeight = 0.0
    private var weightPerSecond = -1.0
    private var weightNeedsRecalculating = true
    private var rankGoal = -1
    private var minAmount = 0.0
    private var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast()
    private var inventoryOpen = false


    // Used to get the difference in weight to subtract for monthly lb
    // Caused by various inaccuracies, including pest calc
    private var shWeightDiff = 0.0
    private var apiWeight = 0.0

    // Calculated weight number to display
    private val displayWeight get() = localWeight + weight - shWeightDiff


    //private val nextPlayer get() = nextPlayers.firstOrNull()
    private var currentLeaderboardType: EliteLeaderboardType
        get() = storage?.lastLeaderboardType ?: EliteLeaderboardType.ALL_TIME
        set(value) {
            value.let {
                GardenApi.storage?.farmingWeight?.lastLeaderboardType = it
            }
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


    fun update() {
        drawDisplay()
        //ChatUtils.debug("Update")
    }


    // TODO format display + draggable config list
    // TODO monthly weight swap button
    private fun drawDisplay() {
        if (!isEnabled()) return

        val lineMap = mutableMapOf<FarmingWeightTextEntry, Renderable>()
        val weight = getWeight(currentLeaderboardType)?.roundTo(2)?.addSeparators() ?: "Loading..."
        val leaderboardPos = getLeaderboardFormat()
        lineMap[FarmingWeightTextEntry.WEIGHT_POSITION] =
            Renderable.clickable(
                "§6$lbName§7: §e$weight$leaderboardPos",
                tips = listOf("§eClick to open your Farming Profile."),
                onLeftClick = { openWebsite(PlayerUtils.getName()) },
            )

        lineMap[FarmingWeightTextEntry.OVERTAKE] = overtakeRenderable()

        display = formatDisplay(lineMap)

    }

    private fun overtakeRenderable(): Renderable {
        val leaderboardPos = getLeaderboardPosition(currentLeaderboardType)
        if (leaderboardPos == 1) return Renderable.text("§bNo players ahead!")
        var (nextName, weightUntil) = getNextPlayer(currentLeaderboardType) ?: return Renderable.text("§bLoading next player...")

        val rankGoal = getRankGoal()
        if (config.useEtaGoalRank.get() && rankGoal != null && rankGoal < (leaderboardPos ?: Int.MAX_VALUE)) {
            nextName += " §7[§b#${rankGoal.addSeparators()}§7]"
        }

        val text = "§e${weightUntil.roundTo(2).addSeparators()} §bbehind $nextName"
        return Renderable.clickable(
            text,
            tips = listOf("§eClick to open the Farming Profile of §b$nextName."),
            onLeftClick = { openWebsite(nextName) },
        )
    }

    private fun formatDisplay(lineMap: MutableMap<FarmingWeightTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeight.apiError) {
            return errorMessage
        }
        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildLeaderboardSwitcher() else newList.addVerticalSpacer()

        newList.addAll(config.text.mapNotNull { lineMap[it] })

        if (inaccurateMilestone) {
            newList.addString("§cOpen §e/cropmilestones §cto update!")
        }
        return newList
    }

    private fun getLeaderboardType() = currentLeaderboardType

    private fun MutableList<Renderable>.buildLeaderboardSwitcher() {
        this.addRenderableButton(
            label = "Leaderboard Type",
            current = getLeaderboardType(),
            onChange = { new ->
                currentLeaderboardType = new
                update()
            },
            universe = EliteLeaderboardType.entries,
        )
    }


    /*private suspend fun update() {
        if (!isEnabled()) return
        if (apiError) {
            display = errorMessage
            return
        }

        /*if (weight == -1.0) loadingWeightMutex.withLock {
            val localProfile = HypixelData.profileName
            if (display.isEmpty()) display = listOf(Renderable.text("§6$lbName§7: §eLoading.."))
            loadWeight(localProfile)
            return
        }*/

        //val weight = getWeight()

        if (rankGoal == -1) rankGoal = getRankGoal()
        val leaderboard = getLeaderboardFormat()

        val list = mutableListOf<Renderable>()
        list.add(
            Renderable.clickable(
                "§6$lbName§7: ${FarmingWeight.weight}$leaderboard",
                tips = listOf("§eClick to open your Farming Profile."),
                onLeftClick = { openWebsite(PlayerUtils.getName()) },
            ),
        )

        if (isEtaEnabled() && (weightPerSecond != -1.0 || config.overtakeETAAlways)) {
            getETA()?.let {
                list.add(it)
            }
        }
        display = list
    }*/

    private fun getLeaderboardFormat(): String {
        val leaderboardPosition = getLeaderboardPosition(currentLeaderboardType)
        if (!config.leaderboard) return ""
        return if (leaderboardPosition != null) {
            val format = leaderboardPosition.addSeparators()
            " §7[§b#$format§7]"
        } else {
            if (loadingLeaderboardMutex.isLocked) " §7[§b#?§7]" else ""
        }
    }
/*
    private fun getRankGoal(): Int {
        val value = config.etaGoalRank
        var goal = 10000 // TODO check if this is causing issues

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
*/
    /*private fun getETA(): Renderable? {
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
                farmingChatMessage("You passed §b$nextName §ein the §6$lbName §eLeaderboard!")
            }

            // Lower leaderboard position
            if (leaderboardPosition == -1) {
                leaderboardPosition = 10000
            } else {
                leaderboardPosition--
            }
            storage?.lastLeaderboard = leaderboardPosition

            // Remove passed player to present the next one
            nextPlayers.removeFirstOrNull()

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
            Renderable.text(text)
        } else {
            Renderable.clickable(
                text,
                tips = listOf("§eClick to open the Farming Profile of §b$nextName."),
                onLeftClick = { openWebsite(nextName) },
            )
        }
    }*/

    private fun resetData() {
        apiError = false
        // We ask both api endpoints after every world switch
        weight = -1.0
        weightPerSecond = -1.0

        //leaderboardPosition = -1
        weightNeedsRecalculating = true
        lastLeaderboardUpdate = SimpleTimeMark.farPast()

        //nextPlayers.clear()
        rankGoal = -1

        localCounter.clear()
    }
/*
    private fun farmingChatMessage(message: String) {
        ChatUtils.hoverableChat(
            message,
            listOf(
                "§eClick to open your Farming Weight",
                "§eprofile on §celitebot.dev",
            ),
            "/shfarmingprofile ${PlayerUtils.getName()}",
        )
    }
*/
    fun isEnabled() = config.display && (inGardenEnabled())
    private fun inGardenEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)

    private fun isEtaEnabled() = config.overtakeETA
    //private fun isMonthlyLB() = config.eliteLBType.get() == EliteFarmingWeightConfig.EliteFarmingWeightLBType.MONTHLY

    /*fun addCrop(crop: CropType, addedCounter: Int) {
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
    }*/
/*
    private fun loadLeaderboardIfAble() {
        if (loadingLeaderboardMutex.isLocked) return
        SkyHanniMod.launchIOCoroutine {
            loadingLeaderboardMutex.withLock {
                val wasNotLoaded = leaderboardPosition == -1
                leaderboardPosition = loadLeaderboardPosition()
                if (wasNotLoaded) checkOffScreenLeaderboardChanges()
                storage?.lastLeaderboard = leaderboardPosition
                lastLeaderboardUpdate = SimpleTimeMark.now()
            }
        }
    }

    private fun checkOffScreenLeaderboardChanges() {
        if (!config.showLbChange) return
        val oldPosition = storage?.lastLeaderboard ?: return
        if (oldPosition <= 0 || leaderboardPosition <= 0) return

        val diff = leaderboardPosition - oldPosition
        if (diff == 0) return
        val verbFormat = if (diff > 0) "§cdropped" else "§arisen"
        val placesFormat = StringUtils.pluralize(abs(diff), "place", withNumber = true)
        farmingChatMessage(
            "§7Since your last visit to the §aGarden§7, " +
                "you have $verbFormat $placesFormat §7on the §d$lbName Leaderboard§7. " +
                "§7(§e#${oldPosition.addSeparators()} §7-> §e#${leaderboardPosition.addSeparators()}§7)",
        )
    }

    private suspend fun loadLeaderboardPosition(): Int {
        // Fetch more upcoming players when the difference between ranks is expected to be tiny
        val upcomingPlayers = when {
            !isEnabled() -> 0
            leaderboardPosition > 10_000 -> 50
            leaderboardPosition > 5_000 -> 30
            leaderboardPosition > 1_000 -> 20
            else -> 10
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

        val lbType = if (isMonthlyLB()) EliteLeaderboardType.MONTHLY else EliteLeaderboardType.NORMAL

        val apiData = EliteDevApi.fetchLeaderboardPositions(
            profileId = profileId,
            lbType = lbType,
            upcomingCount = upcomingPlayers,
            atRank = atRank,
        ) ?: return leaderboardPosition

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
    }
*/
    /*private fun loadWeight(localProfile: String) = SkyHanniMod.launchIOCoroutine {
        /*val apiData = EliteDevApi.fetchWeightProfile(localProfile) ?: run {
            apiError = true
            return@launchIOCoroutine
        }*/
        profileId = "aea28adee98d489eb582638d20e80e23"//apiData.profileId
        //weight = apiData.totalWeight
        localCounter.clear()
        weightNeedsRecalculating = true
    }*/

    /*private fun calculateCollectionWeight(): MutableMap<CropType, Double> {
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
    }*/

    private fun lookUpCommand(it: Array<String>) {
        val name = if (it.size == 1) it[0] else PlayerUtils.getName()
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

    /*private val cropWeight = mutableMapOf<CropType, Double>()
    private var attemptingCropWeightFetch = false
    private var hasFetchedCropWeights = false

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
            cropWeight[cropType] = crop.value
        }
        hasFetchedCropWeights = true
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
    )*/

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shfarmingprofile") {
            description = "Look up the farming profile from yourself or another player on elitebot.dev"
            category = CommandCategory.USERS_ACTIVE
            callback { lookUpCommand(it) }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(1, "garden.eliteFarmingWeightoffScreenDropMessage")
        event.move(3, "garden.eliteFarmingWeightDisplay", "garden.eliteFarmingWeights.display")
        event.move(3, "garden.eliteFarmingWeightPos", "garden.eliteFarmingWeights.pos")
        event.move(3, "garden.eliteFarmingWeightLeaderboard", "garden.eliteFarmingWeights.leaderboard")
        event.move(3, "garden.eliteFarmingWeightOvertakeETA", "garden.eliteFarmingWeights.overtakeETA")
        event.move(3, "garden.eliteFarmingWeightOffScreenDropMessage", "garden.eliteFarmingWeights.offScreenDropMessage")
        event.move(3, "garden.eliteFarmingWeightOvertakeETAAlways", "garden.eliteFarmingWeights.overtakeETAAlways")
        event.move(3, "garden.eliteFarmingWeightETAGoalRank", "garden.eliteFarmingWeights.ETAGoalRank")
        event.move(3, "garden.eliteFarmingWeightIgnoreLow", "garden.eliteFarmingWeights.ignoreLow")
        event.move(14, "garden.eliteFarmingWeight.offScreenDropMessage", "garden.eliteFarmingWeights.showLbChange")
        event.move(34, "garden.eliteFarmingWeights.ETAGoalRank", "garden.eliteFarmingWeights.etaGoalRank")

        val base = "#garden.farmingWeight"
        event.move(101, "$base.lastFarmingWeightLeaderboard", "$base.lastLeaderboard")
    }
}
