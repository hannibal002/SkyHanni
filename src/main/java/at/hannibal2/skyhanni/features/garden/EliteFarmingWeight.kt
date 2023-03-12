package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat

class EliteFarmingWeight {

    @SubscribeEvent
    fun onCollectionUpdate(event: CollectionUpdateEvent) {
        if (isEnabled()) {
            breakBlocksToTryAgain = false
            update()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (isEnabled()) {
            config.eliteFarmingWeightPos.renderString(display)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        if (apiError) {
            apiError = false
            breakBlocksToTryAgain = true
            if (config.eliteFarmingWeightDisplay) {
                update()
            }
        }
    }

    companion object {
        private val config get() = SkyHanniMod.feature.garden
        private val extraCollection = mutableMapOf<String, Long>()
        private var display = "§6Farming Weight§7: §eLoading.."

        private fun isEnabled() = GardenAPI.inGarden() && config.eliteFarmingWeightDisplay

        fun addCrop(crop: String, diff: Int) {
            val old = extraCollection[crop] ?: 0L
            extraCollection[crop] = old + diff
            breakBlocksToTryAgain = false
            if (isEnabled()) {
                update()
            }
        }

        private fun update() = SkyHanniMod.coroutineScope.launch {
            display = "§6Farming Weight§7: " + if (apiError) {
                "§cAPI error!"
            } else if (breakBlocksToTryAgain) {
                "§cBreak blocks to try again!"
            } else {
                val collectionWeight = calculateCollectionWeight()
                val cropWeight = collectionWeight.values.sum()

                try {
                    val totalWeight = cropWeight + getBonusWeight()
                    val format = DecimalFormat("#,##0.00").format(totalWeight)
                    "§e$format"
                } catch (e: Exception) {
                    apiError = true
                    LorenzUtils.error("[SkyHanni] Failed to load farming weight data from elitebot.dev! please report this on discord!")
                    e.printStackTrace()
                    "§cAPI error!"
                }
            }
        }

        private suspend fun getBonusWeight(): Int {
            for (profileEntry in getApiResult()["profiles"].asJsonObject.entrySet()) {
                val profile = profileEntry.value.asJsonObject
                val profileName = profile["cute_name"].asString.lowercase()
                if (profileName == HyPixelData.profile) {
                    return profile["farming"].asJsonObject["bonus"].asInt
                }
            }
            return 0
        }

        private var apiCache: JsonObject? = null
        private var apiError = false
        private var breakBlocksToTryAgain = false

        private suspend fun getApiResult(): JsonObject {
            apiCache?.let { return it }

            val uuid = Minecraft.getMinecraft().thePlayer.uniqueID.toString().replace("-", "")
            val url = "https://elitebot.dev/api/weight/$uuid"
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            apiCache = result
            return result
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
            weightPerCrop["Mushroom"] = specialMushroomWeight(weightPerCrop, totalWeight)
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
            val l = (extraCollection[cropName]
                ?: 0L)
            val ll = CollectionAPI.getCollectionCounter(cropName)?.second ?: 0L
            return (ll + l).toDouble()
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