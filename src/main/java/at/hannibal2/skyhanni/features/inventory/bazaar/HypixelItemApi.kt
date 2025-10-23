package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.SkyblockItemsDataJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuGeorgeJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.api.ApiStaticGetPath
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.json.fromJson
import kotlin.time.Duration.Companion.minutes

class HypixelItemApi {

    @SkyHanniModule
    companion object {

        // prices = george prices + npc prices
        private var prices = mapOf<NeuInternalName, Double>()
        private var npcPrices = mapOf<NeuInternalName, Double>()
        private var georgePrices = mapOf<NeuInternalName, Double>()

        fun getNpcPrice(internalName: NeuInternalName) = prices[internalName]

        @HandleEvent
        fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
            val constant = event.getConstant<NeuGeorgeJson>("george")
            georgePrices = constant.prices ?: return
            prices = georgePrices + npcPrices
        }
    }

    private val hypixelItemStatic = ApiStaticGetPath(
        "https://api.hypixel.net/v2/resources/skyblock/items",
        "Hypixel SkyBlock Items",
    )

    private suspend fun loadItemData() {
        val (_, apiResponseData) = ApiUtils.getJsonResponse(hypixelItemStatic).assertSuccessWithData() ?: return
        val itemsData = ConfigManager.gson.fromJson<SkyblockItemsDataJson>(apiResponseData)

        val npcPrices = mutableMapOf<NeuInternalName, Double>()
        val motesPrice = mutableMapOf<NeuInternalName, Double>()
        val allStats = mutableMapOf<NeuInternalName, Map<String, Int>>()
        for (item in itemsData.items) {
            val neuItemId = NeuItems.transHypixelNameToInternalName(item.id ?: continue)
            item.npcPrice?.let { npcPrices[neuItemId] = it }
            item.motesPrice?.let { motesPrice[neuItemId] = it }
            item.stats?.let { stats -> allStats[neuItemId] = stats }
        }
        ItemUtils.updateBaseStats(allStats)
        RiftApi.motesPrice = motesPrice
        HypixelItemApi.npcPrices = npcPrices
    }

    fun start() {
        SkyHanniMod.launchIOCoroutine("hypixel item api fetch", timeout = 1.minutes) {
            loadItemData()
            prices = georgePrices + npcPrices
        }

        // TODO use SecondPassedEvent
    }

}
