package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Collections

class FarmingWeightDisplay {

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (isEnabled() && config.ignoreLow || weight >= 200) {
            config.pos.renderStrings(display, posLabel = "Farming Weight Display")
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        // Reset speed
        weightPerSecond = -1.0
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        // We want to try to connect to the api again after a world switch.
        apiError = false
        // We ask both api endpoints after every world switch
        weight = -1.0
        weightPerSecond = -1.0

        leaderboardPosition = -1
        dirtyCropWeight = true
        lastLeaderboardUpdate = 0

        nextPlayers.clear()
        rankGoal = -1

        localCounter.clear()
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        profileId = ""
        weight = -2.0

        nextPlayers.clear()
        rankGoal = -1
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        update()

        SkyHanniMod.coroutineScope.launch {
            getCropWeights()
            hasFetchedCropWeights = true
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(1, "garden.eliteFarmingWeightoffScreenDropMessage", "garden.eliteFarmingWeightOffScreenDropMessage")
        event.move(3, "garden.eliteFarmingWeightDisplay","garden.eliteFarmingWeights.display")
        event.move(3, "garden.eliteFarmingWeightPos","garden.eliteFarmingWeights.pos")
        event.move(3, "garden.eliteFarmingWeightLeaderboard","garden.eliteFarmingWeights.leaderboard")
        event.move(3, "garden.eliteFarmingWeightOvertakeETA","garden.eliteFarmingWeights.overtakeETA")
        event.move(3, "garden.eliteFarmingWeightOffScreenDropMessage","garden.eliteFarmingWeights.offScreenDropMessage")
        event.move(3, "garden.eliteFarmingWeightOvertakeETAAlways","garden.eliteFarmingWeights.overtakeETAAlways")
        event.move(3, "garden.eliteFarmingWeightETAGoalRank","garden.eliteFarmingWeights.ETAGoalRank")
        event.move(3, "garden.eliteFarmingWeightIgnoreLow","garden.eliteFarmingWeights.ignoreLow")
    }

    companion object {
        private val config get() = SkyHanniMod.feature.garden.eliteFarmingWeights
        private val localCounter = mutableMapOf<CropType, Long>()

        private var display = emptyList<String>()
        private var profileId = ""
        private var lastLeaderboardUpdate = 0L
        private var apiError = false
        private var leaderboardPosition = -1
        private var weight = -2.0
        private var localWeight = 0.0
        private var weightPerSecond = -1.0
        private var dirtyCropWeight = false
        private var isLoadingWeight = false
        private var isLoadingLeaderboard = false
        private var rankGoal = -1

        private var nextPlayers = mutableListOf<UpcomingPlayer>()
        private val nextPlayer get() = nextPlayers.firstOrNull()

        private fun update() {
            if (!GardenAPI.inGarden()) return
            if (apiError) {
                display = listOf(
                    "§6Farming Weight§7: §cError!",
                    "§cCannot load data from Elite Farmers!",
                    "§eRejoin garden to try again."
                )
                return
            }
            if (weight == -2.0) {
                display = Collections.singletonList("§6Farming Weight§7: §eLoading..")
                return
            }

            if (weight == -1.0) {
                if (!isLoadingWeight) {
                    val localProfile = HypixelData.profileName
                    if (localProfile == "") {
                        display = Collections.singletonList("§cError: profileName is empty!")
                        return
                    }

                    isLoadingWeight = true
                    if (display.isEmpty()) {
                        display = Collections.singletonList("§cLoading..")
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

            val list = mutableListOf<String>()
            list.add("§6Farming Weight§7: $weight$leaderboard")

            if (isEtaEnabled() && (weightPerSecond != -1.0 || config.overtakeETAAlways)) {
                list.add(getETA())
            }
            display = list
        }

        private fun getLeaderboard(): String {
            if (!config.leaderboard) return ""

            // Fetching new leaderboard position every 10.5 minutes
            if (System.currentTimeMillis() > lastLeaderboardUpdate + 630_000) {
                loadLeaderboardIfAble()
            }

            return if (leaderboardPosition != -1) {
                val format = LorenzUtils.formatInteger(leaderboardPosition)
                " §7[§b#$format§7]"
            } else {
                if (isLoadingLeaderboard) " §7[§b#?§7]" else ""
            }
        }

        private fun getWeight(): String {
            if (dirtyCropWeight) {
                val values = calculateCollectionWeight().values
                if (values.isNotEmpty()) {
                    localWeight = values.sum()
                    dirtyCropWeight = false
                }
            }

            val totalWeight = (localWeight + weight)
            return "§e" + LorenzUtils.formatDouble(totalWeight, 2)
        }

        private fun getRankGoal(): Int {
            val value = config.ETAGoalRank
            var goal = 10000

            // Check that the provided string is valid
            val parsed = value.toIntOrNull() ?: 0
            if (parsed < 1 || parsed > goal) {
                LorenzUtils.error("[SkyHanni] Invalid Farming Weight Overtake Goal!")
                LorenzUtils.chat("§eEdit the Overtake Goal config value with a valid number [1-10000] to use this feature!")
                config.ETAGoalRank = goal.toString()
            } else {
                goal = parsed
            }

            // Fetch the positions again if the goal was changed
            if (rankGoal != goal) {
                loadLeaderboardIfAble()
            }

            return goal
        }

        private fun getETA(): String {
            if (weight < 0) return ""

            var nextPlayer = nextPlayer ?: return ""
            var nextName =
                if (leaderboardPosition == -1 || leaderboardPosition > rankGoal) "#$rankGoal" else nextPlayer.name

            val totalWeight = (localWeight + weight)
            var weightUntilOvertake = nextPlayer.weight - totalWeight

            if (weightUntilOvertake < 0) {
                if (weightPerSecond > 0) {
                    farmingChatMessage("§e[SkyHanni] You passed §b$nextName §ein the Farming Weight Leaderboard!")
                }

                // Lower leaderboard position
                if (leaderboardPosition == -1) {
                    leaderboardPosition = 10000
                } else {
                    leaderboardPosition--
                }
                ProfileStorageData.profileSpecific?.garden?.farmingWeight?.lastFarmingWeightLeaderboard =
                    leaderboardPosition

                // Remove passed player to present the next one
                nextPlayers.removeFirst()

                // Display waiting message if nextPlayers list is empty
                nextPlayer = this.nextPlayer ?: return "§cWaiting for leaderboard update..."
                // Update values to next player
                nextName = nextPlayer.name
                weightUntilOvertake = nextPlayer.weight - totalWeight
            }

            if (nextPlayer.weight == 0.0) {
                return "§cRejoin the garden to show ETA!"
            }

            val timeFormat = if (weightPerSecond != -1.0) {
                val timeTillOvertake = (weightUntilOvertake / weightPerSecond) * 1000
                val format = TimeUtils.formatDuration(timeTillOvertake.toLong())
                " §7(§b$format§7)"
            } else ""

            val weightFormat = LorenzUtils.formatDouble(weightUntilOvertake, 2)
            return "§e$weightFormat$timeFormat §7behind §b$nextName"
        }

        private fun farmingChatMessage(message: String) {
            LorenzUtils.hoverableChat(
                message,
                listOf(
                    "§eClick to open your Farming Weight",
                    "§eprofile on §celitebot.dev",
                ),
                "shfarmingprofile ${LorenzUtils.getPlayerName()}"
            )
        }

        private fun isEnabled() = GardenAPI.inGarden() && config.display
        private fun isEtaEnabled() = config.overtakeETA

        fun addCrop(crop: CropType, addedCounter: Int) {
            val before = getExactWeight()
            localCounter[crop] = crop.getLocalCounter() + addedCounter
            val after = getExactWeight()

            updateWeightPerSecond(crop, before, after, addedCounter)

            dirtyCropWeight = true
        }

        private fun updateWeightPerSecond(crop: CropType, before: Double, after: Double, diff: Int) {
            val speed = crop.getSpeed() ?: return
            val weightDiff = (after - before) * 1000
            weightPerSecond = weightDiff / diff * speed / 1000
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
                if (wasNotLoaded && config.offScreenDropMessage) {
                    checkOffScreenLeaderboardChanges()
                }
                ProfileStorageData.profileSpecific?.garden?.farmingWeight?.lastFarmingWeightLeaderboard =
                    leaderboardPosition
                lastLeaderboardUpdate = System.currentTimeMillis()
                isLoadingLeaderboard = false
            }
        }

        private fun checkOffScreenLeaderboardChanges() {
            val profileSpecific = ProfileStorageData.profileSpecific ?: return
            val oldPosition = profileSpecific.garden.farmingWeight.lastFarmingWeightLeaderboard
            if (oldPosition == -1) return

            val diff = leaderboardPosition - oldPosition
            if (diff == 0) return

            if (diff > 0) {
                chatOffScreenChange("§cdropped ${StringUtils.optionalPlural(diff, "place", "places")}", oldPosition)
            } else {
                chatOffScreenChange("§arisen ${StringUtils.optionalPlural(-diff, "place", "places")}", oldPosition)
            }
        }

        private fun chatOffScreenChange(direction: String, oldPosition: Int) {
            farmingChatMessage(
                "§e[SkyHanni] §7Since your last visit to the §aGarden§7, " +
                        "you have $direction §7on the §dFarming Leaderboard§7. " +
                        "§7(§e#${oldPosition.addSeparators()} §7-> §e#${leaderboardPosition.addSeparators()}§7)"
            )
        }

        private suspend fun loadLeaderboardPosition() = try {
            val uuid = LorenzUtils.getPlayerUuid()

            val includeUpcoming = if (isEtaEnabled()) "?includeUpcoming=true" else ""
            val goalRank = getRankGoal() + 1 // API returns upcoming players as if you were at this rank already
            val atRank = if (isEtaEnabled() && goalRank != 10001) "&atRank=$goalRank" else ""

            val url = "https://api.elitebot.dev/leaderboard/rank/farmingweight/$uuid/$profileId$includeUpcoming$atRank"
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject

            if (isEtaEnabled()) {
                nextPlayers.clear()
                // Array of 0-5 upcoming players (or possibly null)
                result["upcomingPlayers"]?.asJsonArray?.let {
                    for (player in it) {
                        val playerData = player.asJsonObject
                        nextPlayers.add(UpcomingPlayer(playerData["ign"].asString, playerData["amount"].asDouble))
                    }
                }
            }

            result["rank"].asInt
        } catch (e: Exception) {
            error()
            e.printStackTrace()
            -1
        }

        private suspend fun loadWeight(localProfile: String) {
            val uuid = LorenzUtils.getPlayerUuid()
            val url = "https://api.elitebot.dev/weight/$uuid"

            try {
                val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
                for (profileEntry in result["profiles"].asJsonArray) {
                    val profile = profileEntry.asJsonObject
                    val profileName = profile["profileName"].asString.lowercase()
                    if (profileName == localProfile) {
                        profileId = profile["profileId"].asString
                        weight = profile["totalWeight"].asDouble

                        localCounter.clear()
                        dirtyCropWeight = true

                        return
                    }
                }
                println("localProfile: '$localProfile'")
                println("url: '$url'")
                println("result: '$result'")
            } catch (e: Exception) {
                println("url: '$url'")
                e.printStackTrace()
            }
            error()
        }

        private fun error() {
            apiError = true
            LorenzUtils.error("[SkyHanni] Loading the farming weight data from elitebot.dev failed!")
            LorenzUtils.chat("§eYou can re-enter the garden to try to fix the problem. If this message repeats, please report it on Discord!")
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
            return factorPerCrop[this] ?: backupFactors[this] ?: error("Crop $this not in backupFactors!")
        }

        fun lookUpCommand(it: Array<String>) {
            val name = if (it.size == 1) it[0] else LorenzUtils.getPlayerName()
            OSUtils.openBrowser("https://elitebot.dev/@$name/")
            LorenzUtils.chat("§e[SkyHanni] Opening Farming Profile of player §b$name")
        }

        private val factorPerCrop = mutableMapOf<CropType, Double>()
        private var attemptingCropWeightFetch = false
        private var hasFetchedCropWeights = false

        private suspend fun getCropWeights() {
            if (attemptingCropWeightFetch || hasFetchedCropWeights) return
            attemptingCropWeightFetch = true

            val url = "https://api.elitebot.dev/weights"
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject

            for (crop in result.entrySet()) {
                val cropType = CropType.getByName(crop.key)
                factorPerCrop[cropType] = crop.value.asDouble
            }
        }

        // still needed when first joining garden and if they cant make https requests
        private val backupFactors by lazy {
            mapOf(
                CropType.WHEAT to 100_000.0,
                CropType.CARROT to 302_061.86,
                CropType.POTATO to 300_000.0,
                CropType.SUGAR_CANE to 200_000.0,
                CropType.NETHER_WART to 250_000.0,
                CropType.PUMPKIN to 98_284.71,
                CropType.MELON to 485_308.47,
                CropType.MUSHROOM to 90_178.06,
                CropType.COCOA_BEANS to 267_174.04,
                CropType.CACTUS to 177_254.45,
            )
        }

        class UpcomingPlayer(val name: String, val weight: Double)
    }
}