package at.hannibal2.skyhanni.data.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.api.ApiStaticGetPath
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.json.fromJson
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HypixelBazaarFetcher {
    private val bzStatic = ApiStaticGetPath(
        "https://api.hypixel.net/v2/skyblock/bazaar",
        "Hypixel Bazaar",
    )
    private val debugConfig get() = SkyHanniMod.feature.dev.debug

    private const val HIDDEN_FAILED_ATTEMPTS = 3

    var latestProductInformation = mapOf<NeuInternalName, BazaarData>()
    private var lastSuccessfulFetch = SimpleTimeMark.farPast()
    private var nextFetchTime = SimpleTimeMark.farPast()
    private var failedAttempts = 0
    private var nextFetchIsManual = false

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Bazaar Data Fetcher from API")

        val data = listOf(
            "failedAttempts: $failedAttempts",
            "nextFetchIsManual: $nextFetchIsManual",
            "nextFetchTime: ${nextFetchTime.timeUntil()}",
            "lastSuccessfulFetch: ${lastSuccessfulFetch.passedSince()}",
        )

        if (failedAttempts == 0) {
            event.addIrrelevant(data)
        } else {
            event.addData(data)
        }
    }

    @HandleEvent
    fun onTick() {
        if (!canFetch()) return
        if (ApiUtils.isBazaarDisabled()) return
        SkyHanniMod.launchIOCoroutine {
            fetchAndProcessBazaarData()
        }
    }

    private suspend fun fetchAndProcessBazaarData() {
        nextFetchTime = SimpleTimeMark.now() + 2.minutes
        val fetchType = if (nextFetchIsManual) "manual" else "automatic"
        nextFetchIsManual = false
        try {
            val (_, jsonResponse) = ApiUtils.getJsonResponse(bzStatic).assertSuccessWithData()
                ?: return onError(fetchType, Exception("Failed to fetch bazaar data from Hypixel API"))
            val response = ConfigManager.gson.fromJson<BazaarApiResponseJson>(jsonResponse)
            if (response.success) {
                latestProductInformation = process(response.products)
                failedAttempts = 0
                lastSuccessfulFetch = SimpleTimeMark.now()
            } else {
                val rawResponse = jsonResponse.toString()
                onError(fetchType, Exception("success=false, cause=${response.cause}"), rawResponse)
            }
        } catch (e: Exception) {
            onError(fetchType, e)
        }
    }

    private fun process(products: Map<String, BazaarProduct>) = products.mapNotNull { (key, product) ->
        val internalName = NeuItems.transHypixelNameToInternalName(key)
        val instantBuyPrice = product.buySummary.minOfOrNull { it.pricePerUnit } ?: 0.0
        val instantSellPrice = product.sellSummary.maxOfOrNull { it.pricePerUnit } ?: 0.0

        if (product.quickStatus.isEmpty()) {
            return@mapNotNull null
        }

        if (internalName.getItemStackOrNull() == null) {
            // Items that exist in Hypixel's Bazaar API, but not in NEU repo (not visible in the ingame bazaar).
            // Should only include Enchants
            if (!isUnobtainableBazaarProduct(key) && debugConfig.printMissingBazaarItems) {
                println("Unknown bazaar product: $key/$internalName")
            }
            return@mapNotNull null
        }
        internalName to BazaarData(internalName.repoItemName, instantBuyPrice, instantSellPrice, product)
    }.toMap()

    private fun isUnobtainableBazaarProduct(key: String): Boolean = when (key) {
        "ENCHANTMENT_COUNTER_STRIKE_3",
        "ENCHANTMENT_COUNTER_STRIKE_4",
        -> true

        else -> false
    }

    private fun BazaarQuickStatus.isEmpty(): Boolean = with(this) {
        sellPrice == 0.0 &&
            sellVolume == 0L &&
            sellMovingWeek == 0L &&
            sellOrders == 0L &&
            buyPrice == 0.0 &&
            buyVolume == 0L &&
            buyMovingWeek == 0L &&
            buyOrders == 0L
    }

    private fun onError(fetchType: String, e: Exception, rawResponse: String? = null) {
        val userMessage = "Failed fetching bazaar price data from hypixel"
        failedAttempts++
        if (failedAttempts <= HIDDEN_FAILED_ATTEMPTS) {
            nextFetchTime = SimpleTimeMark.now() + 15.seconds
            ChatUtils.debug("$userMessage. (errorMessage=${e.message}, failedAttempts=$failedAttempts, $fetchType")
            e.printStackTrace()
        } else {
            nextFetchTime = SimpleTimeMark.now() + 15.minutes
            if (rawResponse == null || rawResponse == "{}") {
                ChatUtils.chat(
                    "§cFailed loading Bazaar Price data!\n" +
                        "§cPlease wait until the Hypixel API is sending correct data again! There is nothing else to do at the moment.",
                    replaceSameMessage = true,
                )
            } else {
                ErrorManager.logErrorWithData(
                    e,
                    userMessage,
                    "fetchType" to fetchType,
                    "failedAttempts" to failedAttempts,
                    "rawResponse" to rawResponse,
                )
            }
        }
    }

    private fun canFetch() = SkyBlockUtils.onHypixel && nextFetchTime.isInPast()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shupdatebazaarprices") {
            description = "Forcefully updating the bazaar prices right now."
            category = CommandCategory.USERS_BUG_FIX
            callback {
                failedAttempts = 0
                nextFetchIsManual = true
                nextFetchTime = SimpleTimeMark.now()
                ChatUtils.chat("Manually updating the bazaar prices right now..")
            }
        }
    }
}
