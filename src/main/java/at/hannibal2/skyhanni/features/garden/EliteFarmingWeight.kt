package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
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
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        // This is still not perfect, but it's definitely better than other alternatives for the moment.
        extraCollection.clear()
        dirtyCropWeight = true
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (isEnabled()) {
            config.eliteFarmingWeightPos.renderStrings(display)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        // We want to try to connect to the api again after a world switch.
        apiError = false
    }

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (tick++ % 5 != 0) return
        update()
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        // Supporting profile switches
        leaderboardPosition = -1
        bonusWeight = -1
        dirtyCropWeight = true
        lastLeaderboardUpdate = 0
        nextPlayerWeight = 0.0
        nextPlayerName = ""
    }

    companion object {
        private val config get() = SkyHanniMod.feature.garden
        private val extraCollection = mutableMapOf<String, Long>()

        private var display = mutableListOf<String>()
        private var profileId = ""
        private var lastLeaderboardUpdate = 0L
        private var apiError = false
        private var leaderboardPosition = -1
        private var bonusWeight = -2
        private var cropWeight = 0.0
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
            if (bonusWeight == -2) {
                display = Collections.singletonList("§6Farming Weight§7: §eLoading..")
                return
            }

            if (bonusWeight == -1) {
                if (!isLoadingWeight) {
                    isLoadingWeight = true
                    SkyHanniMod.coroutineScope.launch {
                        loadBonusWeight()
                        isLoadingWeight = false
                    }
                }
                return
            }

            val weight = getWeight()
            val leaderboard = getLeaderboard()

            val list = mutableListOf<String>()
            list.add("§6Farming Weight§7: $weight$leaderboard")
            if (isEtaEnabled()) {
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
                    cropWeight = values.sum()
                    dirtyCropWeight = false
                }
            }

            val totalWeight = (cropWeight + bonusWeight)
            return "§e" + LorenzUtils.formatDouble(totalWeight, 2)
        }

        private fun getETA(): String {
            val totalWeight = (cropWeight + bonusWeight)
            val weightUntilOvertake = nextPlayerWeight - totalWeight

            val format = LorenzUtils.formatDouble(weightUntilOvertake, 2)
            return "§e$format §7weight left to overtake §b$nextPlayerName"
        }

        private fun isEnabled() = GardenAPI.inGarden() && config.eliteFarmingWeightDisplay
        private fun isEtaEnabled() = config.eliteFarmingWeightOvertakeETA

        fun addCrop(crop: String, diff: Int) {
            val old = extraCollection[crop] ?: 0L
            extraCollection[crop] = old + diff
            dirtyCropWeight = true
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

        private suspend fun loadBonusWeight() {
            val uuid = Minecraft.getMinecraft().thePlayer.uniqueID.toString().replace("-", "")
            val url = "https://elitebot.dev/api/weight/$uuid"
            try {
                val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
                val localProfile = HyPixelData.profileName
                for (profileEntry in result["profiles"].asJsonObject.entrySet()) {
                    val profile = profileEntry.value.asJsonObject
                    val profileName = profile["cute_name"].asString.lowercase()
                    if (profileName == localProfile) {
                        profileId = profileEntry.key
                        bonusWeight = profile["farming"].asJsonObject["bonus"].asInt
                        return
                    }
                }
                println("url: '$url'")
                println("result: '$result'")
            } catch (e: Exception) {
                println("url: '$url'")
                e.printStackTrace()
            }
            apiError = true
            LorenzUtils.error("[SkyHanni] Failed to load farming weight data from elitebot.dev! please report this on discord!")
        }

        private fun calculateCollectionWeight(): MutableMap<String, Double> {
            val weightPerCrop = mutableMapOf<String, Double>()
            var totalWeight = 0.0
            for ((cropName, factor) in factorPerCrop) {
                val collection = getCollection(cropName)
                val weight = (collection / factor).round(2)
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
            val mushroomCollection = getCollection("Mushroom")
            return doubleBreakRatio * (mushroomCollection / (2 * mushroomFactor)) + normalRatio * (mushroomCollection / mushroomFactor)
        }

        private fun getCollection(cropName: String): Double {
            val real = CollectionAPI.getCollectionCounter(cropName)?.second ?: 0L
            val extra = (extraCollection[cropName] ?: 0L)
            return (real + extra).toDouble()
        }

        private val factorPerCrop by lazy {
            mapOf(
                "wheat" to 100_000.0,
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