package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat

class EliteFarmingWeight {

    @SubscribeEvent
    fun onCollectionUpdate(event: CollectionUpdateEvent) {
        if (!config.eliteFarmingWeightDisplay) return
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.eliteFarmingWeightPos.renderString(display)
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.eliteFarmingWeightDisplay

    companion object {
        private val config get() = SkyHanniMod.feature.garden
        private val extraCollection = mutableMapOf<String, Long>()
        private var display = ""

        fun addCrop(crop: String, diff: Int) {
            if (!config.eliteFarmingWeightDisplay) return
            val old = extraCollection[crop] ?: 0L
            extraCollection[crop] = old + diff
            update()
        }

        private fun update() = SkyHanniMod.coroutineScope.launch {
            val collectionWeight = calculateCollectionWeight()
            val cropWeight = collectionWeight.values.sum()

            val bonusWeight = if (apiError) 0 else getBonusWeight()

            val totalWeight = cropWeight + bonusWeight
            val s = DecimalFormat("#,##0.00").format(totalWeight)
            display = "§6Farming Weight§7: §e$s"
        }

        private suspend fun getBonusWeight(): Int {
            for (profileEntry in getApiResult()["profiles"].asJsonObject.entrySet()) {
                val profile = profileEntry.value.asJsonObject
                val profileName = profile["cute_name"].asString
                val profile1 = HyPixelData.profile
                if (profileName.lowercase() == profile1) {
                    return profile["farming"].asJsonObject["bonus"].asInt
                }
            }
            return 0
        }

        private var apiCache: JsonObject? = null
        private var apiError = false

        private suspend fun getApiResult(): JsonObject {
            apiCache?.let { return it }
            val result: JsonObject?
            try {
                val uuid = Minecraft.getMinecraft().thePlayer.uniqueID.toString().replace("-", "")
                val url = "https://elitebot.dev/api/weight/$uuid"
                result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            } catch (e: Error) {
                apiError = true
                e.printStackTrace()
                throw Error(e)
            }
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