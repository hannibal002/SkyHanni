package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarData
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getLowestBinOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NEUItems.getRawCraftCostOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

object ItemPriceUtils {

    fun debugItemPrice(args: Array<String>) {
        val internalName = getItemOrFromHand(args)
        if (internalName == null) {
            ChatUtils.userError("Hold an item in hand or do /shdebugprice <item name/id>")
            return
        }


        val defaultPrice = internalName.getPrice().addSeparators()
        ChatUtils.chat("${internalName.itemName}§f: §6$defaultPrice")

        println("")
        println(" Debug Item Price for $internalName ")
        println("defaultPrice: $defaultPrice")

        println(" #")
        for (source in ItemPriceSource.values()) {
            val price = internalName.getPrice(source)
            println("${source.displayName} price: ${price.addSeparators()}")
        }
        println(" #")

        println(" ")
        println("getLowestBinOrNull: ${internalName.getLowestBinOrNull()?.addSeparators()}")

        internalName.getBazaarData().let {
            println("getBazaarData sellOfferPrice: ${it?.sellOfferPrice?.addSeparators()}")
            println("getBazaarData instantBuyPrice: ${it?.instantBuyPrice?.addSeparators()}")
        }

        println("getNpcPriceOrNull: ${internalName.getNpcPriceOrNull()?.addSeparators()}")
        println("getRawCraftCostOrNull: ${internalName.getRawCraftCostOrNull()?.addSeparators()}")
        println(" ")
    }

    // TODO move either into inventory utils or new command utils
    fun getItemOrFromHand(args: Array<String>): NEUInternalName? {
        val name = args.joinToString(" ")
        return if (name.isEmpty()) {
            InventoryUtils.getItemInHand()?.getInternalName()
        } else {
            val internalName = name.asInternalName()
            if (internalName.getItemStackOrNull() != null) {
                internalName
            } else {
                NEUInternalName.fromItemNameOrNull(name)
            }

        }
    }
}
