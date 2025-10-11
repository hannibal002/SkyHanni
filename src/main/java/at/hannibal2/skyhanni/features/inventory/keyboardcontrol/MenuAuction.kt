package at.hannibal2.skyhanni.features.inventory.keyboardcontrol

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.bindNumberKeysToItems
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.bindNumberKeysToSlots
import at.hannibal2.skyhanni.features.inventory.keyboardcontrol.KeyBinding.Companion.createPatternBindings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object MenuAuction {
    private val config get() = SkyHanniMod.feature.inventory.keyboardControl
    private val patternGroup = RepoPattern.group("keyboardcontrol.auction")

    // -- BUTTONS --

    // Main menu
    private val browserButtonPattern by patternGroup.pattern("button.main.browser", "Auctions Browser")
    private val bidsButtonPatternPrimary by patternGroup.pattern("button.main.bids", "Manage Bids")

    // for whatever reason, name of this button changes when you click it (and go back to see it). Hypixel moment
    private val bidsButtonPatternAlt by patternGroup.pattern("button.main.bidsalt", "View Bids")

    private val manageAuctionsButtonPattern by patternGroup.pattern("button.main.manage", "Manage Auctions")
    private val manageAuctionsButtonPatternAlt by patternGroup.pattern("button.main.managealt", "Auctions Management")

    private val statsButtonPattern by patternGroup.pattern("button.main.stats", "Auction Stats")

    // Browser
    private val searchButtonPattern by patternGroup.pattern("button.search", "Search")
    private val sortButtonPattern by patternGroup.pattern("button.browser.sort", "Sort")
    private val tierFilterButtonPattern by patternGroup.pattern("button.browser.tierfilter", "Item Tier")
    private val binFilterButtonPattern by patternGroup.pattern("button.browser.binfilter", "BIN Filter")

    private val backButtonPattern by patternGroup.pattern("button.back", "Go Back")
    private val closeButtonPattern by patternGroup.pattern("button.close", "Close")
    private val prevPageButtonPattern by patternGroup.pattern("button.page.previous", "Previous Page")
    private val nextPageButtonPattern by patternGroup.pattern("button.page.next", "Next Page")

    /**
     * REGEX-TEST: Claim All
     */
    private val claimButtonPattern by patternGroup.pattern("button.manage.claim", "Claim .*")

    /**
     * REGEX-TEST: Create Auction
     * REGEX-TEST: Create BIN Auction
     */
    private val createAuctionButtonPattern by patternGroup.pattern("button.manage.create", "Create.* Auction")

    /**
     * REGEX-TEST: Confirm
     * REGEX-TEST: Confirm Auction Creation
     */
    private val confirmButtonPattern by patternGroup.pattern("button.confirm.confirm", "Confirm.*")
    // used for different things actually
    /**
     * REGEX-TEST: Cancel Auction
     * REGEX-TEST: Cancel
     */
    private val cancelButtonPattern by patternGroup.pattern("button.confirm.cancel", "Cancel.*")

    /**
     * REGEX-TEST: Item price: 10000
     */
    private val setPriceButtonPattern by patternGroup.pattern("button.create.setprice", "Item price: .*")

    /**
     * REGEX-TEST: Duration: 2 Days
     */
    private val setDurationButtonPattern by patternGroup.pattern("button.create.setduration", "Duration: .*")
    private val customDurationButtonPattern by patternGroup.pattern("button.duration.custom", "Custom Duration")
    private val buyNowButtonPattern by patternGroup.pattern("button.view.buynow", "Buy Item Right Now")
    private val placeBidButtonPattern by patternGroup.pattern("button.view.placebid", "Submit Bid")
    private val cancelAuctionButtonPattern by patternGroup.pattern("button.view.cancel", "Cancel Auction")
    private val collectAuctionButtonPattern by patternGroup.pattern("button.view.collect", "Collect Auction")

    // -- TITLES --
    /**
     * REGEX-TEST: Co-op Auction House
     */
    private val mainMenuTitlePattern by patternGroup.pattern("title.main", ".*Auction House")
    private val browserTitlePattern by patternGroup.pattern("title.browser", "Auctions Browser")
    private val manageAuctionsTitlePattern by patternGroup.pattern("title.manage", "Manage Auctions")

    /**
     * REGEX-TEST: BIN Auction View
     * REGEX-TEST: Auction View
     */
    private val lotViewTitlePattern by patternGroup.pattern("title.view", ".*Auction View")
    private val manageBidsTitlePattern by patternGroup.pattern("title.bids", "Your Bids")

    /**
     * REGEX-TEST: Create BIN Auction
     * REGEX-TEST: Create Auction
     */
    private val createAuctionTitlePattern by patternGroup.pattern("title.create", "Create.* Auction")
    private val confirmPurchaseTitlePattern by patternGroup.pattern("title.confirm.purchase", "Confirm Purchase")

    /**
     * REGEX-TEST: Confirm BIN Auction
     * REGEX-TEST: Confirm Auction
     */
    private val confirmAuctionTitlePattern by patternGroup.pattern("title.confirm.auction", "Confirm.* Auction")
    private val setDurationMenuTitlePattern by patternGroup.pattern("title.duration", "Auction Duration")

    // Selection grid used in browser
    @Suppress("MagicNumber")
    private val itemGridSlots = intArrayOf(
        11, 12, 13, 14, 15, 16,
        20, 21, 22, 23, 24, 25,
        29, 30, 31, 32, 33, 34,
        38, 39, 40, 41, 42, 43,
    )

    private val menus = arrayOf(
        // Main menu
        UiMenu(
            titlePattern = mainMenuTitlePattern,
            buttonPatterns = arrayOf(
                browserButtonPattern,
                bidsButtonPatternPrimary,
                bidsButtonPatternAlt,
                manageAuctionsButtonPattern,
                manageAuctionsButtonPatternAlt,
                closeButtonPattern,
                statsButtonPattern,
            ),
            getBindings = { _ ->
                createPatternBindings {
                    config.auctionHouse.browser to browserButtonPattern
                    // "Manage Bids" button changes name on click-and-return. Hypixel moment
                    config.auctionHouse.manageBids to bidsButtonPatternPrimary
                    config.auctionHouse.manageBids to bidsButtonPatternAlt
                    config.auctionHouse.manageAuctions to manageAuctionsButtonPattern
                    config.auctionHouse.manageAuctions to manageAuctionsButtonPatternAlt
                    // Convenience aliases via option keys for main menu navigation
                    config.shared.number1 to browserButtonPattern
                    config.shared.number2 to bidsButtonPatternPrimary
                    config.shared.number2 to bidsButtonPatternAlt
                    config.shared.number3 to manageAuctionsButtonPattern
                    config.shared.number3 to manageAuctionsButtonPatternAlt
                }
            },
        ),

        // Auction Browser
        UiMenu(
            titlePattern = browserTitlePattern,
            buttonPatterns = arrayOf(
                searchButtonPattern,
                backButtonPattern,
                sortButtonPattern,
                tierFilterButtonPattern,
                binFilterButtonPattern,
                prevPageButtonPattern,
                nextPageButtonPattern,
            ),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.search to searchButtonPattern
                    config.auctionHouse.sort to sortButtonPattern
                    config.auctionHouse.itemTierFilter to tierFilterButtonPattern
                    config.auctionHouse.binFilter to binFilterButtonPattern
                    config.shared.previousPage to prevPageButtonPattern
                    config.shared.nextPage to nextPageButtonPattern
                    config.shared.back to backButtonPattern
                } + bindNumberKeysToSlots(config, itemGridSlots)
            },
        ),

        // Lot view (single item)
        UiMenu(
            titlePattern = lotViewTitlePattern,
            buttonPatterns = arrayOf(
                backButtonPattern,
                buyNowButtonPattern,
                placeBidButtonPattern,
                cancelAuctionButtonPattern,
                collectAuctionButtonPattern,
            ),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.confirm to buyNowButtonPattern
                    config.shared.confirm to placeBidButtonPattern
                    config.auctionHouse.cancelAuction to cancelAuctionButtonPattern
                    config.shared.confirm to collectAuctionButtonPattern
                    config.shared.back to backButtonPattern
                }
            },
        ),

        // Manage Bids
        UiMenu(
            titlePattern = manageBidsTitlePattern,
            buttonPatterns = arrayOf(claimButtonPattern, backButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.shared.claim to claimButtonPattern
                    config.shared.back to backButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),

        // Manage (your/coop) Auctions
        UiMenu(
            titlePattern = manageAuctionsTitlePattern,
            buttonPatterns = arrayOf(
                claimButtonPattern,
                backButtonPattern,
                sortButtonPattern,
                createAuctionButtonPattern,
            ),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.shared.claim to claimButtonPattern
                    config.auctionHouse.sort to sortButtonPattern
                    config.auctionHouse.createAuction to createAuctionButtonPattern
                    config.shared.back to backButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),

        // Create Auction
        UiMenu(
            titlePattern = createAuctionTitlePattern,
            buttonPatterns = arrayOf(
                backButtonPattern,
                createAuctionButtonPattern,
                setPriceButtonPattern,
                setDurationButtonPattern,
            ),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.confirm to createAuctionButtonPattern
                    config.auctionHouse.setPrice to setPriceButtonPattern
                    config.auctionHouse.setDuration to setDurationButtonPattern
                    config.shared.back to backButtonPattern
                }
            },
        ),

        // Confirm Auction
        UiMenu(
            titlePattern = confirmAuctionTitlePattern,
            buttonPatterns = arrayOf(confirmButtonPattern, cancelButtonPattern),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.back to cancelButtonPattern
                    config.shared.confirm to confirmButtonPattern
                    config.shared.back to backButtonPattern
                }
            },
        ),

        // Confirm Purchase
        UiMenu(
            titlePattern = confirmPurchaseTitlePattern,
            buttonPatterns = arrayOf(confirmButtonPattern, cancelButtonPattern),
            getBindings = { _ ->
                createPatternBindings {
                    config.shared.back to cancelButtonPattern
                    config.shared.confirm to confirmButtonPattern
                    config.shared.back to backButtonPattern
                }
            },
        ),

        // Set Duration
        UiMenu(
            titlePattern = setDurationMenuTitlePattern,
            buttonPatterns = arrayOf(customDurationButtonPattern, backButtonPattern),
            getBindings = { snapshot ->
                createPatternBindings {
                    config.auctionHouse.customDuration to customDurationButtonPattern
                    config.shared.back to backButtonPattern
                } + bindNumberKeysToItems(config, snapshot)
            },
        ),
    )


    init {
        Registry.registerMenus(menus)
    }
}
