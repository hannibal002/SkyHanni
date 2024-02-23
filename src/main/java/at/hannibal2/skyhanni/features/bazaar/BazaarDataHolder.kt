package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getDoubleOrNull
import at.hannibal2.skyhanni.utils.getStringOrNull
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
        val itemsData = APIUtil.getJSONResponse("https://api.hypixel.net/v2/resources/skyblock/items")

        try {
            val motesPrice = mutableMapOf<NEUInternalName, Double>()
            for (item in itemsData.getAsJsonArray("items")) {
                val itemData = item.asJsonObject

                val hypixelId = itemData.getStringOrNull("id") ?: continue
                val neuItemId = NEUItems.transHypixelNameToInternalName(hypixelId)

                itemData.getDoubleOrNull("npc_sell_price")?.let {
                    list[neuItemId] = it
                }
                itemData.getDoubleOrNull("motes_sell_price")?.let {
                    motesPrice[neuItemId] = it
                }
            }
            RiftAPI.motesPrice = motesPrice
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(
                e, "Error getting npc sell prices",
                "hypixelApiResponse" to itemsData
            )
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
            ChatUtils.debug("Bazaar data is null: '$internalName'")
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
