package at.lorenz.mod.bazaar

import at.lorenz.mod.utils.LorenzUtils

class BazaarApi {

    companion object {
        private val bazaarMap = mutableMapOf<String, BazaarData>()

        fun isBazaarInventory(inventoryName: String): Boolean {
            if (inventoryName.contains(" ➜ ") && !inventoryName.contains("Museum")) return true
            if (BazaarOrderHelper.isBazaarOrderInventory(inventoryName)) return true

            return when (inventoryName) {
                "Your Bazaar Orders" -> true
                "How many do you want?" -> true
                "How much do you want to pay?" -> true
                "Confirm Buy Order" -> true
                "Confirm Instant Buy" -> true
                "At what price are you selling?" -> true
                "Confirm Sell Offer" -> true
                "Order options" -> true

                else -> false
            }
        }

        fun getCleanBazaarName(name: String): String {
            if (name.endsWith(" Gemstone")) {
                return name.substring(6)
            }
            if (name.startsWith("§")) {
                return name.substring(2)
            }

            return name
        }

        fun getBazaarDataForName(name: String): BazaarData {
            if (bazaarMap.containsKey(name)) {
                val bazaarData = bazaarMap[name]
                if (bazaarData != null) {
                    return bazaarData
                }
                LorenzUtils.error("Bazaar data is null for item '$name'")
            }
            throw Error("no bz data found for name '$name'")
        }

        fun isBazaarItem(name: String): Boolean {
            val bazaarName = getCleanBazaarName(name)
            return bazaarMap.containsKey(bazaarName)
        }
    }

    init {
        BazaarDataGrabber(bazaarMap).start()
    }
}