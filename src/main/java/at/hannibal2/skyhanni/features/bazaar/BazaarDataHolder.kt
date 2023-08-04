package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlinx.coroutines.launch
import kotlin.concurrent.fixedRateTimer

class BazaarDataHolder {

    companion object {
        private val bazaarData = mutableMapOf<NEUInternalName, BazaarData>()
        private var npcPrices = mapOf<NEUInternalName, Double>()
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
        val stack = NEUItems.getItemStackOrNull(internalName.asString())
        if (stack == null) {
            LorenzUtils.debug("Bazaar data is null: '$internalName'")
            return null
        }
        val displayName = stack.name!!.removeColor()
        val sellPrice = NEUItems.getPrice(internalName, true)
        val buyPrice = NEUItems.getPrice(internalName, false)
        val npcPrice = npcPrices[internalName].let {
            if (it == null) {
                if (!ignoreNoNpcPrice(internalName.asString())) {
                    LorenzUtils.debug("NPC price not found for item '$internalName'")
                }
                0.0
            } else it
        }

        val data = BazaarData(displayName, sellPrice, buyPrice, npcPrice)
        bazaarData[internalName] = data
        return data
    }

    private fun ignoreNoNpcPrice(internalName: String): Boolean {
        if (internalName.startsWith("TURBO_")) return true
        if (internalName == "PURPLE_CANDY") return true
        if (internalName == "JACOBS_TICKET") return true
        if (internalName == "RAW_SOULFLOW") return true
        if (internalName == "DERELICT_ASHE") return true

        if (internalName.contains(";")) return true

        return false
    }
}