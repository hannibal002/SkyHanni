package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class EliteFarmingWeight {

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (isEnabled()) {
            config.eliteFarmingWeightPos.renderStrings(display, posLabel = "Elite Farming Weight")
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        // Reset speed
        weightPerSecond = -1.0
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        // We want to try to connect to the api again after a world switch.
        apiError = false
        // We ask both api endpoints after every world switch
        weight = -1.0
        weightPerSecond = -1.0

        leaderboardPosition = -1
        dirtyCropWeight = true
        lastLeaderboardUpdate = 0

        nextPlayers.clear()
        localCounter.clear()
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        profileId = ""
        weight = -2.0
        nextPlayers.clear()
    }

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (tick++ % 5 != 0) return
        update()
    }

    companion object {
        private val config get() = SkyHanniMod.feature.garden
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
                    if (localProfile == "") return

                    isLoadingWeight = true
                    SkyHanniMod.coroutineScope.launch {
                        loadWeight(localProfile)
                        isLoadingWeight = false
                    }
                }
                return
            }

            val weight = getWeight()
            val leaderboard = getLeaderboard()

            val list = mutableListOf<String>()
            list.add("§6Farming Weight§7: $weight$leaderboard")

            if (isEtaEnabled() && (weightPerSecond != -1.0 || config.eliteFarmingWeightOvertakeETAAlways)) {
                list.add(getETA())
            }
            display = list
        }

        private fun getLeaderboard(): String {
            if (!config.eliteFarmingWeightLeaderboard) return ""

            // Fetching new leaderboard position every 10.5 minutes
            if (System.currentTimeMillis() > lastLeaderboardUpdate + 630_000) {
                if (!isLoadingLeaderboard) {
                    isLoadingLeaderboard = true
                    SkyHanniMod.coroutineScope.launch {
                        leaderboardPosition = loadLeaderboardPosition()
                        lastLeaderboardUpdate = System.currentTimeMillis()
                        isLoadingLeaderboard = false
                    }
                }
            }

            return if (leaderboardPosition != -1) {
                val format = LorenzUtils.formatInteger(leaderboardPosition)
                " §7[§b#$format§7]"
            } else {
                if (isLoadingLeaderboard) {
                    " §7[§b#?§7]"
                } else ""
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

        private fun getETA(): String {
            if (weight < 0) return ""

            var next = nextPlayer ?: return ""
            var nextName = if (leaderboardPosition == -1) "#10000" else next.ign

            val totalWeight = (localWeight + weight)
            var weightUntilOvertake = next.amount - totalWeight

            if (weightUntilOvertake < 0) {
                if (weightPerSecond > 0) {
                    LorenzUtils.debug("weightPerSecond: '$weightPerSecond'")
                    LorenzUtils.chat("§e[SkyHanni] You passed §b$nextName §ein the Farming Weight Leaderboard!")
                }

                // Lower leaderboard position
                if (leaderboardPosition == -1) {
                    leaderboardPosition = 10000
                } else {
                    leaderboardPosition--
                }

                // Remove passed player to present the next one
                nextPlayers.removeFirst()

                // Display waiting message if nextPlayers list is empty
                next = nextPlayer ?: return "§cWaiting for leaderboard update..."
                // Update values to next player
                nextName = next.ign
                weightUntilOvertake = next.amount - totalWeight
            }

            if (next.amount == 0.0) {
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

        private fun isEnabled() = GardenAPI.inGarden() && config.eliteFarmingWeightDisplay
        private fun isEtaEnabled() = config.eliteFarmingWeightOvertakeETA

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

        private suspend fun loadLeaderboardPosition() = try {
            val uuid = LorenzUtils.getPlayerUuid()
            val includeUpcoming = if (isEtaEnabled()) "?includeUpcoming=true" else ""
            val url = "https://api.elitebot.dev/leaderboard/rank/farmingweight/$uuid/$profileId$includeUpcoming"
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
            LorenzUtils.chat("§eYou can re-enter the garden to try to fix the problem. If this message repeats itself, please report it on Discord!")
        }

        private fun calculateCollectionWeight(): MutableMap<CropType, Double> {
            val weightPerCrop = mutableMapOf<CropType, Double>()
            var totalWeight = 0.0
            for (crop in CropType.values()) {
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

        private fun CropType.getFactor() = factorPerCrop[this]!!

        private val factorPerCrop by lazy {
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

        data class UpcomingPlayer(val ign: String, val amount: Double)
    }
}