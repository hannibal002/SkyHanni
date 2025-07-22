package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.OwnInventoryData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.bazaar.HypixelBazaarFetcher
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarOpenedProductEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarTransactionEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarTransactionEvent.TransactionType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.equalsIgnoreColor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object BazaarApi {

    private val storage get() = ProfileStorageData.playerSpecific?.bazaar

    private var loadedNpcPriceData = false

    val holder = HypixelItemApi()
    var inBazaarInventory = false
    private var currentSearchedItem = ""

    var currentlyOpenedProduct: NeuInternalName? = null
    private var lastOpenedProduct: NeuInternalName? = null
    var orderOptionProduct: NeuInternalName? = null

    private val patternGroup = RepoPattern.group("inventory.bazaar")

    /**
     * REGEX-TEST: [Bazaar] Bought 1x Small Storage for 3,999.5 coins!
     * REGEX-TEST: [Bazaar] Sold 1x Coal for 4.2 coins!
     * REGEX-TEST: [Bazaar] Buy Order Setup! 1x Coal for 4.4 coins.
     * REGEX-TEST: [Bazaar] Order Flipped! 5x Coal for 13.0 coins of total expected profit.
     * REGEX-TEST: [Bazaar] Sell Offer Setup! 447,199x Spider Essence for 486,999,711 coins.
     */
    @Suppress("MaxLineLength")
    private val transactionPattern by patternGroup.pattern(
        "transaction",
        "\\[Bazaar] (?<type>Bought|Buy Order Setup!|Sold|Sell Offer Setup!|Order Flipped!) [\\d,]+x (?<item>.*) for (?<coins>[\\d,.]+) coins.*",
    )

    /**
     * REGEX-TEST: Bazaar ➜ Coal
     * REGEX-TEST: How many do you want?
     * REGEX-TEST: Confirm Buy Order
     * REGEX-TEST: Confirm Buy Order
     * REGEX-TEST: Order options
     */
    private val inventoryNamePattern by patternGroup.list(
        "inventory-name",
        "Bazaar ➜ .*",
        "How many do you want\\?",
        "How much do you want to pay\\?",
        "Confirm Buy Order",
        "Confirm Instant Buy",
        "At what price are you selling\\?",
        "Confirm Sell Offer",
        "Order options",
    )

    /**
     * REGEX-TEST: Your Bazaar Orders
     * REGEX-TEST: Co-op Bazaar Orders
     */
    private val inventoryBazaarOrdersPattern by patternGroup.list(
        "inventory-bazaar-orders",
        "Your Bazaar Orders",
        "Co-op Bazaar Orders",
    )

    /**
     * REGEX-TEST: §8Current tax: 1%
     */
    private val taxPattern by patternGroup.pattern(
        "instantsell.tax",
        "§8Current tax: (?<tax>[\\d.]+)%",
    )

    private var taxRate: Double
        get() = storage?.taxRate ?: 1.25
        private set(value) {
            storage?.taxRate = value
        }

    fun NeuInternalName.getBazaarData(): BazaarData? = HypixelBazaarFetcher.latestProductInformation[this]

    fun NeuInternalName.getBazaarDataOrError(): BazaarData = getBazaarData() ?: run {
        ErrorManager.skyHanniError(
            "Can not find bazaar data for $repoItemName",
            "internal name" to this,
        )
    }

    fun isBazaarItem(stack: ItemStack): Boolean = stack.getInternalName().isBazaarItem()

    fun NeuInternalName.isBazaarItem() = getBazaarData() != null

    fun searchForBazaarItem(internalName: NeuInternalName, amount: Int = -1) {
        searchForBazaarItem(internalName.itemNameWithoutColor, amount)
    }

    fun searchForBazaarItem(displayName: String, amount: Int? = null) {
        if (!SkyBlockUtils.inSkyBlock) return
        if (NeuItems.neuHasFocus()) return
        if (SkyBlockUtils.noTradeMode) return
        if (DungeonApi.inDungeon() || KuudraApi.inKuudra) return
        HypixelCommands.bazaar(displayName.removeColor())
        amount?.let { OSUtils.copyToClipboard(it.toString()) }
        currentSearchedItem = displayName.removeColor()
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inBazaarInventory = checkIfInBazaar(event)
        if (inBazaarInventory) {
            updateTaxRate(event.inventoryItems)

            val openedProduct = getOpenedProduct(event.inventoryItems) ?: return
            currentlyOpenedProduct = openedProduct
            lastOpenedProduct = openedProduct
            BazaarOpenedProductEvent(openedProduct, event).post()
        }
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        val item = event.item ?: return
        val itemName = item.displayName
        if (isBazaarOrderInventory(InventoryUtils.openInventoryName())) {
            val internalName = item.getInternalNameOrNull() ?: return
            if (itemName.contains("SELL")) {
                orderOptionProduct = internalName
            } else if (itemName.contains("BUY")) {
                // pickup items from bazaar order
                OwnInventoryData.ignoreItem(1.seconds) { it == internalName }
                // prepare for cancel buy order as well
                orderOptionProduct = internalName
            }
        }
        if (InventoryUtils.openInventoryName() == "Order options" && itemName == "§cCancel Order") {
            // pickup items from own bazaar order
            OwnInventoryData.ignoreItem(1.seconds) { it == orderOptionProduct }

        }

        if (inBazaarInventory) {
            if (item.getLore().lastOrNull()?.removeColor() == "Click to buy now!") {
                // instant buy
                OwnInventoryData.ignoreItem(1.seconds) { it == lastOpenedProduct }
            }
        }
    }

    private fun getOpenedProduct(inventoryItems: Map<Int, ItemStack>): NeuInternalName? {
        val buyInstantly = inventoryItems[10] ?: return null

        if (buyInstantly.displayName != "§aBuy Instantly") return null
        val bazaarItem = inventoryItems[13] ?: return null

        return NeuInternalName.fromItemName(bazaarItem.displayName)
    }

    private fun updateTaxRate(inventoryItems: Map<Int, ItemStack>) {
        val sellInstantly = inventoryItems[11] ?: return

        if (sellInstantly.displayName != "§6Sell Instantly") return
        taxPattern.firstMatcher(sellInstantly.getLore()) {
            taxRate = group("tax").formatDouble()
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (ApiUtils.isHypixelItemsDisabled()) return

        if (!loadedNpcPriceData) {
            loadedNpcPriceData = true
            holder.start()
        }
    }

    // TODO cache
    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!inBazaarInventory) return
        if (!SkyHanniMod.feature.inventory.bazaar.purchaseHelper) return
        if (currentSearchedItem == "") return

        if (event.gui !is GuiChest) return
        val chest = event.container as ContainerChest

        for ((slot, stack) in chest.getUpperItems()) {
            if (chest.inventorySlots.indexOf(slot) !in 9..44) {
                continue
            }

            if (stack.displayName.removeColor() == currentSearchedItem) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        val message = event.message.removeColor()
        transactionPattern.matchMatcher(message) {
            val item = group("item")
            val coins = group("coins").formatDoubleOrNull() ?: return
            val coinsAfterTax = when (group("type")) {
                "Sold" -> coins * (1 - taxRate / 100)
                else -> coins
            }
            val transactionType = TransactionType.getByMessageOrNull(group("type"))
            if (transactionType != null) {
                BazaarTransactionEvent(transactionType, coins, coinsAfterTax).post()
            }
            if (currentSearchedItem == item) {
                currentSearchedItem = ""
            }
        }
    }

    private fun checkIfInBazaar(event: InventoryFullyOpenedEvent): Boolean {
        val items = event.inventorySize.let { listOf(it - 5, it - 6) }.mapNotNull { event.inventoryItems[it] }
        if (items.any { it.displayName.equalsIgnoreColor("Go Back") && it.getLore().firstOrNull() == "§7To Bazaar" }) {
            return true
        }

        // check for Buy Instantly
        event.inventoryItems[16]?.let {
            if (it.displayName == "§aCustom Amount" && it.getLore().firstOrNull() == "§8Buy Order Quantity") {
                return true
            }
        }

        if (isBazaarOrderInventory(event.inventoryName)) return true
        return inventoryNamePattern.matches(event.inventoryName)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(25, "bazaar", "inventory.bazaar")
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inBazaarInventory = false
        currentlyOpenedProduct = null
    }

    fun isBazaarOrderInventory(inventoryName: String): Boolean = inventoryBazaarOrdersPattern.matches(inventoryName)
}
