package at.lorenz.mod.bazaar

import at.lorenz.mod.utils.APIUtil
import at.lorenz.mod.utils.LorenzUtils
import at.lorenz.mod.utils.LorenzUtils.round
import kotlin.concurrent.fixedRateTimer

internal class BazaarDataGrabber(private var bazaarMap: MutableMap<String, BazaarData>) {

    companion object {
        private val itemNames = mutableMapOf<String, String>()

        private var lastData = ""
        var lastTime = 0L
        var blockNoChange = false
        var currentlyUpdating = false
    }

    private fun loadItemNames(): Boolean {
        currentlyUpdating = true
        try {
            val itemsData = APIUtil.getJSONResponse("https://api.hypixel.net/resources/skyblock/items")
            for (element in itemsData["items"].asJsonArray) {
                val jsonObject = element.asJsonObject
                val name = jsonObject["name"].asString
                val id = jsonObject["id"].asString
                itemNames[id] = name
            }
            currentlyUpdating = false
            return true
        } catch (e: Throwable) {
            e.printStackTrace()
            LorenzUtils.error("Error while trying to read bazaar item list from api: " + e.message)
            currentlyUpdating = false
            return false
        }
    }

    fun start() {
        fixedRateTimer(name = "lorenz-bazaar-update", period = 1000L) {
            //TODO add
//            if (!LorenzUtils.inSkyBlock) {
//                return@fixedRateTimer
//            }

            if (currentlyUpdating) {
                LorenzUtils.error("Bazaar update took too long! Error?")
                return@fixedRateTimer
            }

            if (itemNames.isEmpty()) {
                if (!loadItemNames()) {
                    return@fixedRateTimer
                }
            }
            checkIfUpdateNeeded()
        }
    }

    private fun checkIfUpdateNeeded() {
        if (lastData != "") {
            if (System.currentTimeMillis() - lastTime > 9_000) {
                blockNoChange = true
            } else {
                if (blockNoChange) {
                    return
                }
            }
        }

        currentlyUpdating = true
        updateBazaarData()
        currentlyUpdating = false
    }

    private fun updateBazaarData() {
        val bazaarData = APIUtil.getJSONResponse("https://api.hypixel.net/skyblock/bazaar")
        if (bazaarData.toString() != lastData) {
            lastData = bazaarData.toString()
            lastTime = System.currentTimeMillis()
        }

        val products = bazaarData["products"].asJsonObject

        for (entry in products.entrySet()) {
            val apiName = entry.key

            if (apiName == "ENCHANTED_CARROT_ON_A_STICK") continue
            if (apiName == "BAZAAR_COOKIE") continue

            val itemData = entry.value.asJsonObject

            val itemName = itemNames.getOrDefault(apiName, null)
            if (itemName == null) {
                LorenzUtils.error("Bazaar item name is null for '$apiName'! Restart to fix this problem!")
                continue
            }

            val sellPrice: Double = try {
                itemData["sell_summary"].asJsonArray[0].asJsonObject["pricePerUnit"].asDouble.round(1)
            } catch (e: Exception) {
//                LorenzUtils.warning("Bazaar buy order for $itemName not found!")
                0.0
            }
            val buyPrice: Double = try {
                itemData["buy_summary"].asJsonArray[0].asJsonObject["pricePerUnit"].asDouble.round(1)
            } catch (e: Exception) {
//                LorenzUtils.warning("Bazaar sell offers for $itemName not found!")
                0.0
            }

            val data = BazaarData(apiName, itemName, sellPrice, buyPrice)
            bazaarMap[itemName] = data
        }
    }
}