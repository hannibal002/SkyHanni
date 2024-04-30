package at.hannibal2.skyhanni.data.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarData
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.fromJson
import com.google.gson.annotations.Expose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// https://api.hypixel.net/#tag/SkyBlock/paths/~1v2~1skyblock~1bazaar/get
object HypixelBazaarFetcher {
    private val url = "https://api.hypixel.net/v2/skyblock/bazaar"
    private val hiddenFailedAttempts = 3

    var latestProductInformation = mapOf<NEUInternalName, BazaarData>()
    private var nextFetchTime = SimpleTimeMark.farPast()
    private var failedAttepmts = 0
    private var nextFetchIsManual = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!canFetch()) return
        SkyHanniMod.coroutineScope.launch {
            fetchAndProcessBazaarData()
        }
    }

    private suspend fun fetchAndProcessBazaarData() {
        nextFetchTime = SimpleTimeMark.now() + 2.minutes
        val fetchType = if (nextFetchIsManual) "manual" else "automatic"
        nextFetchIsManual = false
        try {
            val jsonResponse = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            val response = ConfigManager.gson.fromJson<BazaarApiResponse>(jsonResponse)
            if (response.success) {
                latestProductInformation = process(response.products)
                failedAttepmts = 0
            } else {
                onError(fetchType, Exception("success=false, cause=${response.cause}"))
            }
        } catch (e: Exception) {
            onError(fetchType, e)
        }
    }

    private fun process(products: Map<String, BazaarProduct>) = products.mapNotNull { (key, product) ->
        val internalName = NEUItems.transHypixelNameToInternalName(key)
        val sellOfferPrice = product.buySummary.minOfOrNull { it.pricePerUnit } ?: 0.0
        val insantBuyPrice = product.sellSummary.maxOfOrNull { it.pricePerUnit } ?: 0.0
        if (internalName.getItemStackOrNull() == null) {
            // Items that exist in Hypixel's Bazaar API, but not in NEU repo (not visible in in the ingame bazaar)
            if (LorenzUtils.getPlayerUuid() == "8a9f184148e948edb14f76a124e6c9df" || LorenzUtils.debug)
                println("jani moment: $internalName/$key")
            return@mapNotNull null
        }
        internalName to BazaarData(internalName.itemName, sellOfferPrice, insantBuyPrice, product)
    }.toMap()

    private fun onError(fetchType: String, e: Exception) {
        val userMessage = "Failed fetching bazaar price data from hypixel"
        failedAttepmts++
        if (failedAttepmts <= hiddenFailedAttempts) {
            nextFetchTime = SimpleTimeMark.now() + 15.seconds
            ChatUtils.debug("$userMessage. (errorMessage=${e.message}, failedAttepmts=$failedAttepmts, $fetchType")
            e.printStackTrace()
        } else {
            nextFetchTime = SimpleTimeMark.now() + 15.minutes
            ErrorManager.logErrorWithData(
                e,
                userMessage,
                "fetchType" to fetchType,
                "failedAttepmts" to failedAttepmts,
            )
        }
    }

    fun fetchNow() {
        failedAttepmts = 0
        nextFetchIsManual = true
        nextFetchTime = SimpleTimeMark.now()
        ChatUtils.chat("Manually updating the bazaar prices right now..")
    }

    private fun canFetch() = LorenzUtils.onHypixel && nextFetchTime.isInPast()

    class BazaarApiResponse(
        @Expose
        val success: Boolean,
        @Expose
        val cause: String,
        @Expose
        val lastUpdated: Long,
        @Expose
        val products: Map<String, BazaarProduct>,
    )

}
