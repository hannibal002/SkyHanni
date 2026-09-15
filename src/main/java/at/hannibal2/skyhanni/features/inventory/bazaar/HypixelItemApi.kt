package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.SkyblockItemsDataJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuGeorgeJson
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.api.ApiStaticGetPath
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.json.fromJson
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HypixelItemApi {
    private val hypixelItemStatic = ApiStaticGetPath(
        "https://api.hypixel.net/v2/resources/skyblock/items",
        "Hypixel SkyBlock Items",
    )

    private const val HIDDEN_FAILED_ATTEMPTS = 3

    private val itemFetchCoroutine = CoroutineSettings("hypixel item api fetch", timeout = 1.minutes)

    // prices = george prices + npc prices
    private var prices = mapOf<NeuInternalName, Double>()
    private var npcPrices = mapOf<NeuInternalName, Double>()
    private var georgePrices = mapOf<NeuInternalName, Double>()
    private var minionStorageXP = mapOf<NeuInternalName, Map<String, Double>>()

    private val isFetching = AtomicBoolean(false)
    private var lastSuccessfulFetch = SimpleTimeMark.farPast()
    private var nextFetchTime = SimpleTimeMark.farPast()
    private var failedAttempts = 0

    fun getNpcPrice(internalName: NeuInternalName) = prices[internalName]

    fun getMinionStorageXP(internalName: NeuInternalName): Map<String, Double>? =
        minionStorageXP[internalName]

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Hypixel Items Data Fetcher from API")

        val data = listOf(
            "failedAttempts: $failedAttempts",
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
    private fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val constant = event.getConstant<NeuGeorgeJson>("george")
        georgePrices = constant.prices ?: return
        prices = georgePrices + npcPrices
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!canFetch()) return
        itemFetchCoroutine.launch { fetchAndProcessItemData() }
    }

    private suspend fun fetchAndProcessItemData() {
        if (!isFetching.compareAndSet(expectedValue = false, newValue = true)) return

        nextFetchTime = SimpleTimeMark.now() + 1.hours
        try {
            val (_, jsonResponse) = ApiUtils.getJsonResponse(hypixelItemStatic).assertSuccessWithData()
                ?: return onError(Exception("Failed to fetch item data from Hypixel API"))
            val itemsData = ConfigManager.gson.fromJson<SkyblockItemsDataJson>(jsonResponse)
            processItemData(itemsData)
            failedAttempts = 0
            lastSuccessfulFetch = SimpleTimeMark.now()
        } catch (e: Exception) {
            onError(e)
        } finally {
            isFetching.store(false)
        }
    }

    internal fun processItemData(itemsData: SkyblockItemsDataJson) {
        val npcPrices = mutableMapOf<NeuInternalName, Double>()
        val motesPrice = mutableMapOf<NeuInternalName, Double>()
        val allStats = mutableMapOf<NeuInternalName, Map<String, Int>>()
        val minionStorageXP = mutableMapOf<NeuInternalName, Map<String, Double>>()
        for (item in itemsData.items) {
            val neuItemId = NeuItems.transHypixelNameToInternalName(item.id ?: continue)
            item.npcPrice?.let { npcPrices[neuItemId] = it }
            item.motesPrice?.let { motesPrice[neuItemId] = it }
            item.stats?.let { stats -> allStats[neuItemId] = stats }
            item.experience?.mapNotNull { (skill, experience) ->
                experience.minionStorage?.let { skill to it }
            }?.toMap()?.takeIf { it.isNotEmpty() }?.let { minionStorageXP[neuItemId] = it }
        }
        ItemUtils.updateBaseStats(allStats)
        RiftApi.motesPrice = motesPrice
        this.npcPrices = npcPrices
        this.minionStorageXP = minionStorageXP
        prices = georgePrices + npcPrices
    }

    private fun onError(e: Exception) {
        val userMessage = "Failed fetching item data from hypixel"
        failedAttempts++
        if (failedAttempts <= HIDDEN_FAILED_ATTEMPTS) {
            nextFetchTime = SimpleTimeMark.now() + 15.seconds
            ChatUtils.debug("$userMessage. (errorMessage=${e.message}, failedAttempts=$failedAttempts)")
            e.printStackTrace()
        } else {
            nextFetchTime = SimpleTimeMark.now() + 15.minutes
            ErrorManager.logErrorWithData(
                e,
                userMessage,
                "failedAttempts" to failedAttempts,
            )
        }
    }

    private fun canFetch() = !ApiUtils.isHypixelItemsDisabled() &&
        SkyBlockUtils.onHypixel &&
        nextFetchTime.isInPast()
}
