package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.SignUtils.getSignLines
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiEditSign

enum class SignType(val contents: Array<String?>) {
    GENERIC_QUERY_1(
        arrayOf(
            null,
            null,
            "^^^^^^^^^^^^^^^",
            "Enter query",
        ),
    ),
    GENERIC_QUERY_2(
        // used in: /recipe search
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter query",
            null,
        ),
    ),
    GENERIC_SEARCH(
        // used in: beastiary, experimentation table, party finder search, dungeon RNG meter
        arrayOf(
            null,
            "^^^^^^",
            "Enter your",
            "search!",
        ),
    ),
    AUCTION_HOUSE(
        arrayOf(
            "Auction House",
            "Click to open",
            "the auction house",
            "menu.",
        ),
    ),
    AH_CREATE_AUCTION_HOURS(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Auction",
            "hours",
        ),
    ),
    AH_CREATE_AUCTION_MINUTES(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Auction",
            "minutes",
        ),
    ),
    AH_CREATE_AUCTION_START_BID(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Your auction",
            "starting bid",
        ),
    ),
    AH_SET_BID(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "auction bid",
            "amount",
        ),
    ),
    BAZAAR_BUY_AMOUNT(
        // insta buy and buy order
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter amount",
            "to order",
        ),
    ),
    BAZAAR_BUY_PRICE(
        // in bazaar buy order
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter price",
            "big nerd",
        ),
    ),
    BAZAAR_SELL_AMOUNT(
        // in bazaar sell order (there is no option to select amount in insta sell)
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter amount",
            "to sell",
        ),
    ),
    BAZAAR_SELL_PRICE(
        // in bazaar sell order
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter price",
            "per unit",
        ),
    ),
    BANK_DEPOSIT(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter the amount",
            "to deposit",
        ),
    ),
    BANK_WITHDRAW(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter the amount",
            "to withdraw",
        ),
    ),
    PLAYER_TRADE_COIN_AMOUNT(
        arrayOf(
            null,
            "^^^^^^",
            "Enter amount",
            "----------------",
        ),
    ),
    SUPERCRAFT_AMOUNT(
        arrayOf(
            null,
            "^^^^^^",
            "Enter amount",
            "of crafts",
        ),
    ),
    MOUSEMAT_SET_ANGLE(
        arrayOf(
            null,
            "Set Yaw Above!",
            "Set Pitch Below!",
            null,
        ),
    ),
    RANCHER_BOOTS_SET_SPEED(
        arrayOf(
            null,
            "^^^^^^",
            "Set your",
            "speed cap!",
        ),
    ),
    PARTY_FINDER_LEVEL_REQUIREMENT(
        // when starting your own qeue
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter level",
            "requirement!",
        ),
    ),
    PARTY_FINDER_SEARCH_MIN_LEVEL(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter",
            "min level!",
        ),
    ),
    PARTY_FINDER_SEARCH_MAX_LEVEL(
        arrayOf(
            null,
            "^^^^^^^^^^^^^^^",
            "Enter",
            "max level!",
        ),
    ),

    EMPTY(arrayOf("", "", "", "")), // also attributet when Neu "Search GUI" is open
    ISLAND_BLOCK(arrayOf(null, null, null, null)),
    UNKNOWN(arrayOf(null, null, null, null));

    companion object {
        fun fromGuiScreen(gui: GuiScreen): SignType? {
            if (gui !is GuiEditSign) return null
            val signText = gui.getSignLines() ?: return null
            return fromContents(signText)
        }

        fun fromContents(contents: List<String?>): SignType {
            for (type in SignType.entries) {
                if (type.contents.size != contents.size) continue
                var isSame = true
                for (i in type.contents.indices) {
                    if (type.contents[i] != null && type.contents[i] != contents[i]) {
                        isSame = false
                        break
                    }
                }
                if (isSame) return type
            }
            return if (IslandType.PRIVATE_ISLAND.isInIsland()) ISLAND_BLOCK else UNKNOWN
        }
    }
}
