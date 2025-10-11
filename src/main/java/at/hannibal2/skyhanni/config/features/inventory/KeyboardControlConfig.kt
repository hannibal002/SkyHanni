package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.input.Keyboard

class KeyboardControlConfig {

    @Expose
    @ConfigOption(name = "Keybinds", desc = "Enable keyboard controls for Bazaar, Auction House, Sacks, and other inventories.")
    @ConfigEditorBoolean
    @SearchTag("keyboard")
    @FeatureToggle
    var keybindsEnabled: Boolean = false

    @Expose
    @ConfigOption(name = "Keybind cooldown", desc = "Set minimum time (ms) between two distinct clicks in the same inventory.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 300f, minStep = 1f)
    var clickCooldown: Float = 150f

    @Expose
    @ConfigOption(name = "Render", desc = "Render available keybinds on corresponding slots in inventories.")
    @ConfigEditorBoolean
    var renderEnabled: Boolean = false

    @Expose
    @ConfigOption(name = "Selector", desc = "Enable keyboard controlled selector that works in all inventories.")
    @ConfigEditorBoolean
    var selectorEnabled: Boolean = false

    @Expose
    @ConfigOption(name = "Shared Keybinds", desc = "")
    @Accordion
    var shared: SharedConfig = SharedConfig()

    class SharedConfig {
        @Expose
        @ConfigOption(name = "Back", desc = "Go back button.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_C)
        var back: Int = Keyboard.KEY_C

        @Expose
        @ConfigOption(name = "Confirm", desc = "Confirm actions like purchases, bids, orders, etc.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RETURN)
        var confirm: Int = Keyboard.KEY_RETURN

        @Expose
        @ConfigOption(name = "Search", desc = "Open search in Bazaar or Auction Browser.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
        var search: Int = Keyboard.KEY_S

        @Expose
        @ConfigOption(name = "Claim All", desc = "Claim coins from orders/bids.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RETURN)
        var claim: Int = Keyboard.KEY_RETURN

        @Expose
        @ConfigOption(name = "Previous Page", desc = "Go to previous page in (auction/recipes/etc.) browser.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_P)
        var previousPage: Int = Keyboard.KEY_P

        @Expose
        @ConfigOption(name = "Next Page", desc = "Go to next page in (auction/recipes/etc.) browser.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_N)
        var nextPage: Int = Keyboard.KEY_N

        @Expose
        @ConfigOption(name = "Number 1", desc = "Select first item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_1)
        var number1: Int = Keyboard.KEY_1

        @Expose
        @ConfigOption(name = "Number 2", desc = "Select second item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_2)
        var number2: Int = Keyboard.KEY_2

        @Expose
        @ConfigOption(name = "Number 3", desc = "Select third item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_3)
        var number3: Int = Keyboard.KEY_3

        @Expose
        @ConfigOption(name = "Number 4", desc = "Select fourth item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_4)
        var number4: Int = Keyboard.KEY_4

        @Expose
        @ConfigOption(name = "Number 5", desc = "Select fifth item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_5)
        var number5: Int = Keyboard.KEY_5

        @Expose
        @ConfigOption(name = "Number 6", desc = "Select sixth item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_6)
        var number6: Int = Keyboard.KEY_6

        @Expose
        @ConfigOption(name = "Number 7", desc = "Select seventh item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_7)
        var number7: Int = Keyboard.KEY_7

        @Expose
        @ConfigOption(name = "Number 8", desc = "Select eighth item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_8)
        var number8: Int = Keyboard.KEY_8

        @Expose
        @ConfigOption(name = "Number 9", desc = "Select ninth item or option.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_9)
        var number9: Int = Keyboard.KEY_9
    }

    @Expose
    @ConfigOption(name = "Bazaar Keybinds", desc = "")
    @SearchTag("bz")
    @Accordion
    var bazaar: BazaarConfig = BazaarConfig()

    class BazaarConfig {
        @Expose
        @ConfigOption(name = "Manage Orders", desc = "Open Manage Orders menu.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RETURN)
        var manageOrders: Int = Keyboard.KEY_RETURN

        @Expose
        @ConfigOption(name = "Cancel Order", desc = "Cancel selected order.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_X)
        var cancelOrder: Int = Keyboard.KEY_X

        @Expose
        @ConfigOption(name = "Flip Order", desc = "Flip selected order.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_F)
        var flipOrder: Int = Keyboard.KEY_F

        @Expose
        @ConfigOption(name = "Buy Instantly", desc = "Buy item instantly.")
        @SearchTag("instabuy")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        var buyInstantly: Int = Keyboard.KEY_NONE

        @Expose
        @ConfigOption(name = "Sell Instantly", desc = "Sell item instantly.")
        @SearchTag("instasell")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        var sellInstantly: Int = Keyboard.KEY_NONE

        @Expose
        @ConfigOption(name = "Create Buy Order", desc = "Open Buy Order setup.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_B)
        var createBuyOrder: Int = Keyboard.KEY_B

        @Expose
        @ConfigOption(name = "Create Sell Offer", desc = "Open Sell Offer setup.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
        var createSellOffer: Int = Keyboard.KEY_S

        @Expose
        @ConfigOption(name = "View Graphs", desc = "View price graphs for item.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_V)
        var viewGraphs: Int = Keyboard.KEY_V

        @Expose
        @ConfigOption(name = "Ignore Item", desc = "Ignore selected item.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_I)
        var ignoreItem: Int = Keyboard.KEY_I
    }

    @Expose
    @ConfigOption(name = "Auction House Keybinds", desc = "")
    @SearchTag("ah")
    @Accordion
    var auctionHouse: AuctionHouseConfig = AuctionHouseConfig()

    class AuctionHouseConfig {
        @Expose
        @ConfigOption(name = "Browser", desc = "Open Auction Browser from main menu.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
        var browser: Int = Keyboard.KEY_A

        @Expose
        @ConfigOption(name = "Manage Bids", desc = "Open Manage Bids from main menu.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_B)
        var manageBids: Int = Keyboard.KEY_B

        @Expose
        @ConfigOption(name = "Manage Auctions", desc = "Open Manage Auctions from main menu.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_M)
        var manageAuctions: Int = Keyboard.KEY_M

        @Expose
        @ConfigOption(name = "Sort", desc = "Change sort options in Browser or Manage Auctions.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
        var sort: Int = Keyboard.KEY_S

        @Expose
        @ConfigOption(name = "Item Tier Filter", desc = "Change item tier filter in Browser.")
        @SearchTag("rarity")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_T)
        var itemTierFilter: Int = Keyboard.KEY_T

        @Expose
        @ConfigOption(name = "BIN Filter", desc = "Change BIN filter in Browser.")
        @SearchTag("buy it now")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_B)
        var binFilter: Int = Keyboard.KEY_B

        @Expose
        @ConfigOption(name = "Create Auction", desc = "Start creating auction in Manage Auctions.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RETURN)
        var createAuction: Int = Keyboard.KEY_RETURN

        @Expose
        @ConfigOption(name = "Cancel Auction", desc = "Cancel auction in view menu.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_X)
        var cancelAuction: Int = Keyboard.KEY_X

        @Expose
        @ConfigOption(name = "Set Price", desc = "Open price setup in Create Auction.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_P)
        var setPrice: Int = Keyboard.KEY_P

        @Expose
        @ConfigOption(name = "Set Duration", desc = "Open duration setup in Create Auction.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
        var setDuration: Int = Keyboard.KEY_D

        @Expose
        @ConfigOption(name = "Custom Duration", desc = "Select custom duration in duration menu.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
        var customDuration: Int = Keyboard.KEY_D
    }

    @Expose
    @ConfigOption(name = "Sacks Keybinds", desc = "")
    @SearchTag("sax")
    @Accordion
    var sacks: SacksConfig = SacksConfig()

    class SacksConfig {
        @Expose
        @ConfigOption(name = "Insert Inventory", desc = "Insert inventory into Sack of Sacks or specific sack.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RETURN)
        var insertInventory: Int = Keyboard.KEY_RETURN

        @Expose
        @ConfigOption(name = "Pickup All", desc = "Pickup all items from sack.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_P)
        var pickupAll: Int = Keyboard.KEY_P
    }

    @Expose
    @ConfigOption(name = "Recipes Keybinds", desc = "")
    @SearchTag("supercraft")
    @Accordion
    var recipes: RecipesConfig = RecipesConfig()

    class RecipesConfig {
        @Expose
        @ConfigOption(name = "Supercraft Recipes", desc = "Use Supercraft in Recipes menu. Hold SHIFT or CTRL to max/custom.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RETURN)
        var supercraft: Int = Keyboard.KEY_RETURN
    }

    @Expose
    @ConfigOption(name = "Inventory Selector", desc = "")
    @SearchTag("cursor")
    @Accordion
    var inventorySelector: InventorySelectorConfig = InventorySelectorConfig()

    class InventorySelectorConfig {
        @Expose
        @ConfigOption(name = "Remember Position", desc = "Remember selector position per (newly opened) menu.")
        @SearchTag("cache, memorize")
        @ConfigEditorBoolean
        var rememberPosition: Boolean = false

        @Expose
        @ConfigOption(name = "Wrap Selector", desc = "Wrap selector around inventories or clamp it on border.")
        @ConfigEditorBoolean
        var wrap: Boolean = false

        @Expose
        @ConfigOption(name = "Click", desc = "Click selected slot.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RSHIFT)
        var click: Int = Keyboard.KEY_RSHIFT

        @Expose
        @ConfigOption(name = "Move Up", desc = "Move selector up.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_UP)
        var up: Int = Keyboard.KEY_UP

        @Expose
        @ConfigOption(name = "Move Down", desc = "Move selector down.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_DOWN)
        var down: Int = Keyboard.KEY_DOWN

        @Expose
        @ConfigOption(name = "Move Left", desc = "Move selector left.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LEFT)
        var left: Int = Keyboard.KEY_LEFT

        @Expose
        @ConfigOption(name = "Move Right", desc = "Move selector right.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_RIGHT)
        var right: Int = Keyboard.KEY_RIGHT
    }
}
