package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.isBazaarItem
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemPriceUtils.isAuctionHouseItem
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

object ItemBuyApi {

    fun NeuInternalName.buy(amount: Int) {
        when {
            isBazaarItem() -> BazaarApi.searchForBazaarItem(repoItemName, amount)
            isAuctionHouseItem() -> HypixelCommands.auctionSearch(repoItemName.removeColor())
            else -> ChatUtils.chat("Could not find $repoItemName§e on AH or BZ!", replaceSameMessage = true)
        }
    }

    fun NeuInternalName.createBuyTip() = listOf(
        when {
            isBazaarItem() -> "§eClick to search for $repoItemName §ein Bazaar!"
            isAuctionHouseItem() -> "§eClick to search for $repoItemName §ein Auction House!"
            else -> "§cCould not find $repoItemName §eon AH or BZ!"
        },
    )
}
