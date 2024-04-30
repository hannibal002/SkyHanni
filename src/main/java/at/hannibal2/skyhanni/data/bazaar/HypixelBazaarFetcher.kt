package at.hannibal2.skyhanni.data.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.fromJson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object HypixelBazaarFetcher {
    private val url = "https://api.hypixel.net/v2/skyblock/bazaar"
    private val maxFailedAttepmts = 3

    private var latestProductInformation = mapOf<NEUInternalName, BazaarProduct>()
    private var nextFetchTime = SimpleTimeMark.farPast()
    private var failedAttepmts = 0
    private var nextFetchIsManual = false

    init {
        SkyHanniMod.coroutineScope.launch {
            while (true) {
                if (!canFetch()) continue
                if (failedAttepmts == maxFailedAttepmts) continue
                fetchAndProcessBazaarData()
            }
        }
    }

    private fun fetchAndProcessBazaarData() {
        println("fetchAndProcessBazaarData")
        nextFetchTime = SimpleTimeMark.now() + 2.minutes

        val fetchType = if (nextFetchIsManual) "manual" else "automatic"
        nextFetchIsManual = false

        try {
            val jsonResponse = APIUtil.getJSONResponse(url)
            val response = ConfigManager.gson.fromJson<BazaarApiResponse>(jsonResponse)
            if (response.success) {
                latestProductInformation = response.products
                    .mapKeys { NEUItems.transHypixelNameToInternalName(it.key) }
            }
            failedAttepmts = 0
        } catch (e: Exception) {
            // TOOD remove
            e.printStackTrace()
            failedAttepmts++
            if (failedAttepmts <= maxFailedAttepmts) {
                ChatUtils.debug(
                    "Error fetching bazaar price data $fetchType from hypixel: ${e.message} " +
                        "(failedAttepmts=$failedAttepmts)"
                )
                nextFetchTime = SimpleTimeMark.now() + 15.seconds
            } else {
                ErrorManager.logErrorWithData(
                    e,
                    message = "Error fetching bazaar price data from hypixel after $failedAttepmts attempts",
                    "fetchType" to fetchType,
                    betaOnly = true
                )
            }
        }
    }

    fun fetchNow() {
        nextFetchIsManual = true
        nextFetchTime = SimpleTimeMark.now()
    }

    private fun canFetch() = LorenzUtils.onHypixel && nextFetchTime.isInPast()

    class BazaarApiResponse(
        @Expose
        val success: Boolean,
        @Expose
        val lastUpdated: Long,
        @Expose
        val products: Map<String, BazaarProduct>,
    )

    data class BazaarProduct(
        @Expose
        @SerializedName("product_id")
        val productId: String,
        @Expose
        @SerializedName("quick_status")
        val quickStatus: QuickStatus,
        @Expose
        @SerializedName("sell_summary")
        val sellSummary: List<Summary>,
        @Expose
        @SerializedName("buy_summary")
        val buySummary: List<Summary>,
    )

    data class Summary(
        @Expose
        val amount: Long,
        @Expose
        val pricePerUnit: Double,
        @Expose
        val orders: Long,
    )

    class QuickStatus(
        @Expose
        val productId: String,
        @Expose
        val sellPrice: Double,
        @Expose
        val sellVolume: Long,
        @Expose
        val sellMovingWeek: Long,
        @Expose
        val sellOrders: Long,
        @Expose
        val buyPrice: Double,
        @Expose
        val buyVolume: Long,
        @Expose
        val buyMovingWeek: Long,
        @Expose
        val buyOrders: Long,
    )
}
