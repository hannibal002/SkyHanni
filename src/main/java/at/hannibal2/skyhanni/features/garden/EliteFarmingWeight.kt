package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class EliteFarmingWeight {

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (isEnabled()) {
            config.eliteFarmingWeightPos.renderStrings(display, center = false)
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
        nextPlayerWeight = 0.0
        nextPlayerName = ""
    }

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (tick++ % 5 != 0) return
        update()
    }

    companion object {
        private val config get() = SkyHanniMod.feature.garden
        private val localCollection = mutableMapOf<String, Long>()

        private var display = mutableListOf<String>()
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

        private var nextPlayerName = ""
        private var nextPlayerWeight = 0.0

        private fun update() {
            if (!GardenAPI.inGarden()) return
            if (apiError) {
                display = Collections.singletonList("§6Farming Weight§7: §cAPI Error!")
                return
            }
            if (weight == -2.0) {
                display = Collections.singletonList("§6Farming Weight§7: §eLoading..")
                return
            }

            if (weight == -1.0) {
                if (!isLoadingWeight) {
                    val localProfile = HyPixelData.profileName
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
            if (isEtaEnabled() && weightPerSecond != -1.0) {
                list.add(getETA())
            }
            display = list
        }

        private fun getLeaderboard(): String {
            if (!config.eliteFarmingWeightLeaderboard) return ""

            // Fetching new leaderboard position every 10 minutes
            if (System.currentTimeMillis() > lastLeaderboardUpdate + 600_000) {
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

            val totalWeight = (localWeight + weight)
            if (nextPlayerWeight == 0.0) {
                return "§cRejoin the garden to show ETA!"
            }

            val weightUntilOvertake = nextPlayerWeight - totalWeight
            val timeTillOvertake = (weightUntilOvertake / weightPerSecond) * 1000
            val timeFormat = TimeUtils.formatDuration(timeTillOvertake.toLong())

            val format = LorenzUtils.formatDouble(weightUntilOvertake, 2)

            // TODO Maybe add next player name?
//            val nextName = if (leaderboardPosition == -1) "#1000" else nextPlayerName
            val nextName = if (leaderboardPosition == -1) "#1000" else "#" + (leaderboardPosition - 1)
            return "§e$format §7(§b$timeFormat§7) §7behind §b$nextName"
        }

        private fun isEnabled() = GardenAPI.inGarden() && config.eliteFarmingWeightDisplay
        private fun isEtaEnabled() = config.eliteFarmingWeightOvertakeETA

        fun addCrop(crop: String, diff: Int) {
            val old = localCollection[crop] ?: 0L

            val before = calculateExactWeight()
            localCollection[crop] = old + diff
            val after = calculateExactWeight()

            updateWeightPerSecond(crop, before, after, diff)

            dirtyCropWeight = true
        }

        private fun updateWeightPerSecond(crop: String, before: Double, after: Double, diff: Int) {
            val speed = GardenAPI.cropsPerSecond[crop]!!
            if (speed != -1) {
                val weightDiff = (after - before) * 1000
                weightPerSecond = weightDiff / diff * speed / 1000
            }
        }

        private fun calculateExactWeight(): Double {
            val values = calculateCollectionWeight(false).values
            return if (values.isNotEmpty()) {
                values.sum()
            } else 0.0
        }

        private suspend fun loadLeaderboardPosition() = try {
            val uuid = Minecraft.getMinecraft().thePlayer.uniqueID.toString().replace("-", "")
            val showNext = if (isEtaEnabled()) "?showNext=true" else ""
            val url = "https://elitebot.dev/api/leaderboard/rank/weight/farming/$uuid/$profileId$showNext"
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject

            if (isEtaEnabled()) {
                result["next"]?.asJsonObject?.let {
                    nextPlayerName = it["ign"].asString
                    nextPlayerWeight = it["amount"].asDouble
                }
            }

            result["rank"].asInt
        } catch (e: Exception) {
            apiError = true
            LorenzUtils.error("[SkyHanni] Failed to load farming weight data from elitebot.dev! please report this on discord!")
            e.printStackTrace()
            -1
        }

        private fun UUID.uuidToString(): String {
            return "$this"
        }

        private suspend fun loadWeight(localProfile: String) {
            val thePlayer = Minecraft.getMinecraft().thePlayer
            val uniqueID = thePlayer.uniqueID
            val abc = uniqueID.uuidToString()
            val uuid = abc.replace("-", "")
            val url = "https://elitebot.dev/api/weight/$uuid"

            try {
                val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
                for (profileEntry in result["profiles"].asJsonObject.entrySet()) {
                    val profile = profileEntry.value.asJsonObject
                    val profileName = profile["cute_name"].asString.lowercase()
                    if (profileName == localProfile) {
                        profileId = profileEntry.key
                        weight = profile["farming"].asJsonObject["total"].asDouble

                        localCollection.clear()
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
            apiError = true
            LorenzUtils.error("[SkyHanni] Failed to load farming weight data from elitebot.dev! please report this on discord!")
        }

        private fun calculateCollectionWeight(round: Boolean = true): MutableMap<String, Double> {
            val weightPerCrop = mutableMapOf<String, Double>()
            var totalWeight = 0.0
            for ((cropName, factor) in factorPerCrop) {
                val collection = getLocalCollection(cropName)
                val weight = (collection / factor).also { if (round) weight.round(2) else weight }
                weightPerCrop[cropName] = weight
                totalWeight += weight
            }
            if (totalWeight > 0) {
                weightPerCrop["Mushroom"] = specialMushroomWeight(weightPerCrop, totalWeight)
            }
            return weightPerCrop
        }

        private fun specialMushroomWeight(weightPerCrop: MutableMap<String, Double>, totalWeight: Double): Double {
            val cactusWeight = weightPerCrop["Cactus"]!!
            val sugarCaneWeight = weightPerCrop["Sugar Cane"]!!
            val doubleBreakRatio = (cactusWeight + sugarCaneWeight) / totalWeight;
            val normalRatio = (totalWeight - cactusWeight - sugarCaneWeight) / totalWeight;

            val mushroomFactor = factorPerCrop["Mushroom"]!!
            val mushroomCollection = getLocalCollection("Mushroom")
            return doubleBreakRatio * (mushroomCollection / (2 * mushroomFactor)) + normalRatio * (mushroomCollection / mushroomFactor)
        }

        private fun getLocalCollection(cropName: String): Long {
            return localCollection[cropName] ?: 0L
        }

        private val factorPerCrop by lazy {
            mapOf(
                "Wheat" to 100_000.0,
                "Carrot" to 300_000.0,
                "Potato" to 300_000.0,
                "Sugar Cane" to 200_000.0,
                "Nether Wart" to 250_000.0,
                "Pumpkin" to 87_095.11,
                "Melon" to 435_466.47,
                "Mushroom" to 168_925.53,
                "Cocoa Beans" to 257_214.64,
                "Cactus" to 169_389.33,
            )
        }
    }
}