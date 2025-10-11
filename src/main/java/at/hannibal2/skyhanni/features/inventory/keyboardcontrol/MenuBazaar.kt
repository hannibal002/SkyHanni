package at.hannibal2.skyhanni.features.inventory.keyboardcontrol

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.bindNumberKeysToItems
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.bindNumberKeysToSlots
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.createPatternBindings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object MenuBazaar {
    private val config get() = SkyHanniMod.feature.inventory.keyboardControl
    private val patternGroup = RepoPattern.group("keyboardcontrol.bazaar")

    // -- BUTTONS --

    // Main menu
    private val manageOrdersButtonPattern by patternGroup.pattern("button.main.manageorders", "Manage Orders")
    private val searchButtonPattern by patternGroup.pattern("button.search", "Search")

    // Manage orders menu
    private val claimAllButtonPattern by patternGroup.pattern("button.manageorders.claimall", "Claim All Coins")

    // Order options menu
    private val cancelOrderButtonPattern by patternGroup.pattern("button.orderoptions.cancel", "Cancel Order")
    private val flipOrderButtonPattern by patternGroup.pattern("button.orderoptions.flip", "Flip Order")

    // (specific) Item options
    private val buyInstantlyButtonPattern by patternGroup.pattern("button.item.buyinstantly", "Buy Instantly")
    private val sellInstantlyButtonPattern by patternGroup.pattern("button.item.sellinstantly", "Sell Instantly")
    private val buyOrderButtonPattern by patternGroup.pattern("button.item.createbuyorder", "Create Buy Order")
    private val sellOfferButtonPattern by patternGroup.pattern("button.item.createselloffer", "Create Sell Offer")
    private val viewGraphsButtonPattern by patternGroup.pattern("button.item.viewgraphs", "View Graphs")
    private val ignoreItemButtonPattern by patternGroup.pattern("button.item.ignore", "Instasell Ignore")

    // Confirmations
    // For whatever reason, confirm insta buy for custom amount is called custom amount
    private val confirmInstaBuyPattern by patternGroup.pattern("button.confirm.instabuy", "Custom Amount")

    // But confirm for insta sell (if any), is called just "Confirm" (same as its title!)
    private val confirmInstaSellPattern by patternGroup.pattern("button.confirm.instasell", "Confirm")
    private val confirmBuyOrderButtonPattern by patternGroup.pattern("button.confirm.buyorder", "Buy Order")
    private val confirmSellOfferButtonPattern by patternGroup.pattern("button.confirm.selloffer", "Sell Offer")

    // Shared buttons
    private val goBackButtonPattern by patternGroup.pattern("button.back", "Go Back")
    private val sellSacksNowButtonPattern by patternGroup.pattern("button.item.sellsacks", "Sell Sacks Now")
    private val AdvancedModeButtonPattern by patternGroup.pattern("button.item.advancedmode", "Advanced Mode")
    private val sellInventoryNowButtonPattern by patternGroup.pattern("button.item.sellinventory", "Sell Inventory Now")
    private val closeButtonPattern by patternGroup.pattern("button.close", "Close")

    // -- TITLES --

    // Confirm instasell is just confirm. Hypixel really likes exceptions
    // We are really lucky there is nothing else with such name...
    private val confirmInstaSellTitlePattern by patternGroup.pattern("title.confirm.instasell", "Confirm")
    private val confirmInstaBuyTitlePattern by patternGroup.pattern("title.confirm.instabuy", "Confirm Instant Buy")

    /**
     * REGEX-TEST: Bazaar ➜ Oddities
     * REGEX-TEST: Bazaar ➜ Inferno Minion
     */
    private val mainBazaarTitlePattern by patternGroup.pattern("title.main", "Bazaar ➜ .*")
    private val buyQuantityTitlePattern by patternGroup.pattern("title.buy.quantity", "How many do you want\\?")
    private val buyPriceTitlePattern by patternGroup.pattern("title.buy.price", "How much do you want to pay\\?")
    private val confirmBuyTitlePattern by patternGroup.pattern("title.confirm.buyorder", "Confirm Buy Order")

    /**
     * REGEX-TEST: Confirm Instant Buy
     * REGEX-TEST: Confirm Instant Sell
     */
    private val confirmSellTitlePattern by patternGroup.pattern("title.confirm.selloffer", "Confirm Sell Offer")
    private val sellPriceTitlePattern by patternGroup.pattern("title.sell.price", "At what price are you selling\\?")
    private val orderOptionsTitlePattern by patternGroup.pattern("title.orderoptions", "Order options")

    /**
     * REGEX-TEST: Co-op Bazaar Orders
     */
    private val bazaarOrdersTitlePattern by patternGroup.pattern("title.manageorders", ".*Bazaar Orders")

    // We could hardcode (aka explicitly list in some regex) all submenu names,
    // but such system would require manual fixing once Hypixel adds new ones
    // Instead, we detect submenu as *Not* item menus, which have buy/sell buttons
    /**
     * REGEX-TEST: Reaper Pepper ➜ Instant Buy
     * REGEX-TEST: Bazaar ➜ Inferno Minion
     */
    private val categoryMenuTitlePattern by patternGroup.pattern("title.category", ".* ➜ .*")

    // fixed selection grid used on Bazaar main/search screen
    @Suppress("MagicNumber")
    private val searchItemSelectionSlots = intArrayOf(
        11, 12, 13, 14, 15, 16,
        20, 21, 22, 23, 24, 25,
        29, 30, 31, 32, 33, 34,
        38, 39, 40, 41, 42, 43,
    )

    private val menus = arrayOf(
        // Main menu
        UiMenu(
            titlePattern = mainBazaarTitlePattern,
            buttonPatterns = arrayOf(
                manageOrdersButtonPattern,
                searchButtonPattern,
                goBackButtonPattern,
                closeButtonPattern,
            ),
            getBindings = { _ ->
                createPatternBindings {
                    config.bazaar.manageOrders to manageOrdersButtonPattern
                    config.shared.search to searchButtonPattern
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToSlots(config, searchItemSelectionSlots)
            },
        ),

        // Manage orders
        UiMenu(
            titlePattern = bazaarOrdersTitlePattern,
            buttonPatterns = arrayOf(claimAllButtonPattern, goBackButtonPattern, closeButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.shared.claim to claimAllButtonPattern
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),

        // Specific order options
        UiMenu(
            titlePattern = orderOptionsTitlePattern,
            buttonPatterns = arrayOf(
                cancelOrderButtonPattern,
                flipOrderButtonPattern,
                goBackButtonPattern,
                closeButtonPattern,
            ),
            getBindings = { _ ->
                createPatternBindings {
                    config.bazaar.cancelOrder to cancelOrderButtonPattern
                    config.bazaar.flipOrder to flipOrderButtonPattern
                    config.shared.back to goBackButtonPattern
                }
            },
        ),

        // shared 4-option menus (quantity | price, e.g. Stack/160/1024/Custom | Same/+0.1/+5%/Custom)
        UiMenu(
            titlePattern = buyQuantityTitlePattern,
            buttonPatterns = arrayOf(goBackButtonPattern, closeButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),

        UiMenu(
            titlePattern = buyPriceTitlePattern,
            buttonPatterns = arrayOf(goBackButtonPattern, closeButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),

        UiMenu(
            titlePattern = sellPriceTitlePattern,
            buttonPatterns = arrayOf(goBackButtonPattern, closeButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.shared.back to goBackButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),

        // Confirm buy
        UiMenu(
            titlePattern = confirmBuyTitlePattern,
            buttonPatterns = arrayOf(goBackButtonPattern, confirmBuyOrderButtonPattern, closeButtonPattern),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.confirm to confirmBuyOrderButtonPattern
                    config.shared.back to goBackButtonPattern
                }
            },
        ),

        // Confirm sell
        UiMenu(
            titlePattern = confirmSellTitlePattern,
            buttonPatterns = arrayOf(confirmSellOfferButtonPattern, goBackButtonPattern, closeButtonPattern),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.confirm to confirmSellOfferButtonPattern
                    config.shared.back to goBackButtonPattern
                }
            },
        ),

        // Confirm instasell
        UiMenu(
            titlePattern = confirmInstaSellTitlePattern,
            buttonPatterns = arrayOf(confirmInstaSellPattern, goBackButtonPattern, closeButtonPattern),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.confirm to confirmInstaSellPattern
                    config.shared.back to goBackButtonPattern
                }
            },
        ),

        // Confirm instabuy
        UiMenu(
            titlePattern = confirmInstaBuyTitlePattern,
            buttonPatterns = arrayOf(confirmInstaBuyPattern, goBackButtonPattern, closeButtonPattern),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.confirm to confirmInstaBuyPattern
                    config.shared.back to goBackButtonPattern
                }
            },
        ),

        // Category (e.g. Fuels) / item-options (Fuels ➜ Oil Barrel) screen
        UiMenu(
            titlePattern = categoryMenuTitlePattern,
            buttonPatterns = arrayOf(
                buyInstantlyButtonPattern,
                sellInstantlyButtonPattern,
                buyOrderButtonPattern,
                sellOfferButtonPattern,
                viewGraphsButtonPattern,
                ignoreItemButtonPattern,
                manageOrdersButtonPattern,
                goBackButtonPattern,
                sellInventoryNowButtonPattern,
                closeButtonPattern,
                sellSacksNowButtonPattern,
                AdvancedModeButtonPattern,
            ),
            variantIndicators = setOf(buyInstantlyButtonPattern, buyOrderButtonPattern, sellOfferButtonPattern),
            getBindings = { snapshot ->
                if (snapshot.isVariantMenu) {
                    createPatternBindings {
                        config.bazaar.buyInstantly to buyInstantlyButtonPattern
                        config.bazaar.sellInstantly to sellInstantlyButtonPattern
                        config.bazaar.createBuyOrder to buyOrderButtonPattern
                        config.bazaar.createSellOffer to sellOfferButtonPattern
                        config.bazaar.viewGraphs to viewGraphsButtonPattern
                        config.bazaar.ignoreItem to ignoreItemButtonPattern
                        config.bazaar.manageOrders to manageOrdersButtonPattern
                        config.shared.back to goBackButtonPattern
                    }
                } else {
                    createPatternBindings {
                        config.shared.back to goBackButtonPattern
                    } + bindNumberKeysToItems(config, snapshot)
                }
            },
        ),
    )

    init {
        Registry.registerMenus(menus)
    }
}
