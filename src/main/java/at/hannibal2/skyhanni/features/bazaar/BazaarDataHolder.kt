package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlinx.coroutines.launch
import kotlin.concurrent.fixedRateTimer

class BazaarDataHolder {

    companion object {
        private val bazaarData = mutableMapOf<NEUInternalName, BazaarData>()
        private var npcPrices = mapOf<NEUInternalName, Double>()

        fun getNpcPrice(internalName: NEUInternalName) = npcPrices[internalName]
    }

    private fun loadNpcPrices(): MutableMap<NEUInternalName, Double> {
        val list = mutableMapOf<NEUInternalName, Double>()
        try {
            val itemsData = APIUtil.getJSONResponse("https://api.hypixel.net/resources/skyblock/items")
            val motesPrice = mutableMapOf<NEUInternalName, Double>()
            for (element in itemsData["items"].asJsonArray) {
                val jsonObject = element.asJsonObject
                val hypixelId = jsonObject["id"].asString
                jsonObject["npc_sell_price"]?.let {
                    val neuItemId = NEUItems.transHypixelNameToInternalName(hypixelId)
                    list[neuItemId] = it.asDouble
                }
                jsonObject["motes_sell_price"]?.let {
                    val neuItemId = NEUItems.transHypixelNameToInternalName(hypixelId)
                    motesPrice[neuItemId] = it.asDouble
                }
            }
            RiftAPI.motesPrice = motesPrice
        } catch (e: Throwable) {
            e.printStackTrace()
            LorenzUtils.error("Error while trying to read bazaar item list from api: " + e.message)
        }
        return list
    }

    fun start() {
        SkyHanniMod.coroutineScope.launch {
            npcPrices = loadNpcPrices()
        }

        fixedRateTimer(name = "skyhanni-bazaar-update", period = 10_000L) {
            bazaarData.clear()
        }
    }

    fun getData(internalName: NEUInternalName) = bazaarData[internalName] ?: createNewData(internalName)

    private fun createNewData(internalName: NEUInternalName): BazaarData? {
        val stack = internalName.getItemStackOrNull()
        if (stack == null) {
            LorenzUtils.debug("Bazaar data is null: '$internalName'")
            return null
        }
        val displayName = stack.name!!.removeColor()
        val sellPrice = internalName.getPrice(true)
        val buyPrice = internalName.getPrice(false)

        val data = BazaarData(displayName, sellPrice, buyPrice)
        bazaarData[internalName] = data
        return data
    }
}