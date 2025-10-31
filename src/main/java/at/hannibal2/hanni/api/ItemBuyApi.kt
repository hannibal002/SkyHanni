package at.hannibal2.hanni.api

import at.hannibal2.hanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.hanni.features.inventory.bazaar.BazaarApi.isBazaarItem
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.ItemPriceUtils.isAuctionHouseItem
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.StringUtils.removeColor

object ItemBuyApi {

    fun NeuInternalName.buy(amount: Int) {
        when {
            isBazaarItem() -> BazaarApi.searchForBazaarItem(repoItemName, amount)
            isAuctionHouseItem() -> HypixelCommands.auctionSearch(repoItemName.removeColor())
            else -> ChatUtils.chat("Could not find $repoItemName§e on AH or BZ!", replaceSameMessage = true)
        }
    }

    fun NeuInternalName.createBuyTipLine(additionalAction: String = "") = when {
        isBazaarItem() -> "§e${additionalAction}Click to search for $repoItemName §ein Bazaar!"
        isAuctionHouseItem() -> "§e${additionalAction}Click to search for $repoItemName §ein Auction House!"
        else -> "§cCould not find $repoItemName §eon AH or BZ!"
    }

    fun NeuInternalName.createBuyTip() = listOf(createBuyTipLine())

}
