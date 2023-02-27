package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarApi {

    companion object {
        val bazaarMap = mutableMapOf<String, BazaarData>()
        var inBazaarInventory = false

        fun getCleanBazaarName(name: String): String {
            if (name.endsWith(" Gemstone")) {
                return name.substring(6)
            }
            return name.replace("-", " ").removeColor()
        }

        fun getBazaarDataForName(name: String): BazaarData? {
            if (bazaarMap.containsKey(name)) {
                val bazaarData = bazaarMap[name]
                if (bazaarData != null) {
                    return bazaarData
                }
                LorenzUtils.error("Bazaar data not found! '$name'")
            }
            return null
        }

        fun isBazaarItem(stack: ItemStack): Boolean {
            val internalName = stack.getInternalName()
            return bazaarMap.any { it.value.apiName == internalName }

        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inBazaarInventory = checkIfInBazaar(event)
    }

    private fun checkIfInBazaar(event: InventoryOpenEvent): Boolean {
        val returnItem = event.inventorySize - 5
        for ((slot, item) in event.inventoryItems) {
            if (slot == returnItem) {
                if (item.name?.removeColor().let { it == "Go Back" }) {
                    val lore = item.getLore()
                    if (lore.getOrNull(0)?.removeColor().let { it == "To Bazaar" }) {
                        return true
                    }
                }
            }
        }

        if (event.inventoryName.startsWith("Bazaar ➜ ")) return true
        return when (event.inventoryName) {
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

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inBazaarInventory = false
    }

    init {
        BazaarDataGrabber(bazaarMap).start()
    }
}