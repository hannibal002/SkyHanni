package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.events.BazaarUpdateEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.concurrent.fixedRateTimer

internal class BazaarDataGrabber(private var bazaarMap: MutableMap<String, BazaarData>) {

    companion object {
        private val itemNames = mutableMapOf<String, String>()

        var lastTime = 0L
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
                itemNames[id] = name.removeColor()
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
        fixedRateTimer(name = "skyhanni-bazaar-update", period = 1000L) {
            if (!LorenzUtils.inSkyBlock) {
                return@fixedRateTimer
            }

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
        if (System.currentTimeMillis() > lastTime + 10_000) {
            currentlyUpdating = true
            lastTime = System.currentTimeMillis()
            updateBazaarData()
            currentlyUpdating = false
        }
    }

    private fun updateBazaarData() {
        val bazaarData = APIUtil.getJSONResponse("https://api.hypixel.net/skyblock/bazaar")

        val products = bazaarData["products"].asJsonObject

        for (entry in products.entrySet()) {
            var apiName = entry.key

            //TODO use repo
            if (apiName == "ENCHANTED_CARROT_ON_A_STICK") continue
            if (apiName == "BAZAAR_COOKIE") continue

            val itemData = entry.value.asJsonObject
            val sellPrice = try {
                itemData["sell_summary"].asJsonArray[0].asJsonObject["pricePerUnit"].asDouble.round(1)
            } catch (e: Exception) {
                0.0
            }
            val buyPrice: Double = try {
                itemData["buy_summary"].asJsonArray[0].asJsonObject["pricePerUnit"].asDouble.round(1)
            } catch (e: Exception) {
                0.0
            }

            val itemName = getItemName(apiName)
            if (itemName == null) {
                LorenzUtils.warning("§c[SkyHanni] bazaar item '$apiName' not found! Try restarting your minecraft to fix this.")
                continue
            }

            val sellMovingWeek = itemData["quick_status"].asJsonObject["sellMovingWeek"].asInt
            val buyMovingWeek = itemData["quick_status"].asJsonObject["buyMovingWeek"].asInt
            //parse bazaar api format into internal name format
            if (apiName.startsWith("ENCHANTMENT_")) {
                val split = apiName.split("_")
                val last = split.last()
                val dropLast = split.drop(1).dropLast(1)
                val text = dropLast.joinToString("_") + ";" + last
                apiName = text
            }

            val data = BazaarData(apiName, itemName, sellPrice, buyPrice, buyMovingWeek, sellMovingWeek)
            bazaarMap[itemName] = data
        }
        BazaarUpdateEvent(bazaarMap).postAndCatch()
    }

    private fun getItemName(apiName: String): String? {
        var itemName = itemNames.getOrDefault(apiName, null)

        //Crimson Essence
        //ESSENCE_CRIMSON
        return itemName ?: if (apiName.startsWith("ESSENCE_")) {
            val type = apiName.split("_")[1].firstLetterUppercase()
            itemName = "$type Essence"
            itemNames[apiName] = itemName
            itemName
        } else {
            if (apiName.startsWith("ENCHANTMENT_ULTIMATE_") && !apiName.contains("JERRY") && !apiName.contains("WISE")) {
                val enchantmentName = getEnchantmentRealName(apiName.split("ENCHANTMENT_ULTIMATE_")[1])
                itemNames[apiName] = enchantmentName
                enchantmentName
            } else if (apiName.startsWith("ENCHANTMENT_")) {
                val enchantmentName = getEnchantmentRealName(apiName.split("ENCHANTMENT_")[1])
                itemNames[apiName] = enchantmentName
                enchantmentName
            } else {
                null
            }
        }
    }

    private fun getEnchantmentRealName(rawName: String): String {
        val builder = StringBuilder()
        for (word in rawName.lowercase().split("_")) {
            if (word.isInt()) {
                val number = word.toInt()
                if (number < 1) continue
                builder.append(number.toRoman())
            } else {
                if (word in listOf("of", "the")) {
                    builder.append(word)
                } else {
                    builder.append(word.firstLetterUppercase())
                }
            }
            builder.append(" ")
        }
        val string = builder.toString()
        return string.substring(0, string.length - 1)
    }
}