package at.hannibal2.skyhanni.data.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.fromJson
import com.google.gson.annotations.Expose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object HypixelBazaarFetcher {
    private val url = "https://api.hypixel.net/v2/skyblock/bazaar"
    private val maxFailedAttepmts = 3

    private var latestProductInformation = mapOf<NEUInternalName, BazaarProduct>()
    private var nextFetchTime = SimpleTimeMark.farPast()
    private var failedAttepmts = 0
    private var nextFetchIsManual = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!canFetch()) return
        if (failedAttepmts >= maxFailedAttepmts) return
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
                latestProductInformation = response.products
                    .mapKeys { NEUItems.transHypixelNameToInternalName(it.key) }
                failedAttepmts = 0
            } else {
                onError(fetchType, Exception("response has success = false"))
            }
        } catch (e: Exception) {
            onError(fetchType, e)
        }
    }

    private fun onError(fetchType: String, e: Exception) {
        failedAttepmts++
        if (failedAttepmts <= maxFailedAttepmts) {
            ChatUtils.debug(
                "Error fetching bazaar price data $fetchType from hypixel: ${e.message} " +
                    "(failedAttepmts=$failedAttepmts)"
            )
            e.printStackTrace()
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
        val lastUpdated: Long,
        @Expose
        val products: Map<String, BazaarProduct>,
    )

}
